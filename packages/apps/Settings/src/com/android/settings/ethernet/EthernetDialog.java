/*
 * Copyright (C) 2020 SIMCOM Corp.
 *
 * The main interface to control ethernet setting param.
 * Athor: Mr. Tsao Bo, SIMCOM.
 * Restructured on 2020.11.10, for USB ethernet( A.K.A, eth1 ) connection.
 *
 */

package com.android.settings.ethernet;

import com.android.settings.R;
import com.android.settings.ethernet.ip.IPView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.net.IpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.os.Environment;
import android.util.SparseArray;
import android.net.StaticIpConfiguration;
import android.net.EthernetManager;
import android.text.TextUtils;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.os.RemoteException;
import com.android.settings.Utils;
import android.widget.Toast;
import android.net.EthernetManager;
import android.provider.Settings.Global;
import com.zanshang.wifi.ExtraWifiService;
import android.provider.Settings;
import android.widget.Button;


import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.regex.Pattern;


class EthernetDialog extends AlertDialog implements DialogInterface.OnClickListener, DialogInterface.OnShowListener,
        DialogInterface.OnDismissListener{
    private final String TAG = "EthernetDialog";
    private static final boolean localLOGV = true;


    private View mView;
    private RadioButton mIpTypeDhcp;
    private RadioButton mIpTypeManual;
    private RadioButton eth0Radio;
    private RadioButton eth1Radio;

    private StaticIpViews mStaticIpViews;
    private RJ45StaticIpViews mRJ45StaticIpViews;
    private UsbCardStaticIpViews mUsbCardStaticIpViews;
    
    
    private Context mContext;
    private EthernetManager mEthManager;
    private ConnectivityManager mCM;
	
    public EthernetDialog(Context context,EthernetManager EthManager,ConnectivityManager cm) {
        super(context);
        mContext = context;
        mEthManager = EthManager;
        mCM = cm;
        
        createDialogContent();
        
        setOnShowListener(this);
        setOnDismissListener(this);

    }

    public void onShow(DialogInterface dialog) {
        if (localLOGV) Log.d(TAG, "onShow");
        
        UpdateViewContent();
        
        // soft keyboard pops up on the disabled EditText. Hide it.
        InputMethodManager imm = (InputMethodManager)mContext.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mView.getWindowToken(),
                InputMethodManager.HIDE_IMPLICIT_ONLY);

        /* Tsao Bo:The reason we listen click event on postive_button's View rather than the button */
        /* is because we'd like to manually handle the dialog dismiss.*/
        Button positive_button = getButton(BUTTON_POSITIVE);
        positive_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {         
                handle_saveconf();
            }
        });
    }

    public void onClick(DialogInterface dialog, int which){
        
    }
    
    public void onDismiss(DialogInterface dialog) {
        if (localLOGV) Log.d(TAG, "onDismiss");
    }

    private void createDialogContent() {
        this.setTitle(R.string.eth_config_title);
        this.setView(mView = getLayoutInflater().inflate(R.layout.ethernet_configure, null));

        eth0Radio = (RadioButton) mView.findViewById(R.id.eth0_radio);
        eth1Radio = (RadioButton) mView.findViewById(R.id.eth1_radio);
        
        mIpTypeDhcp = (RadioButton) mView.findViewById(R.id.dhcp_radio);
        mIpTypeManual = (RadioButton) mView.findViewById(R.id.manual_radio);
        
        IPView Ipaddr = (IPView)mView.findViewById(R.id.ipaddr_edit);
        IPView NetMask = (IPView)mView.findViewById(R.id.mask_edit);
        IPView Gw = (IPView)mView.findViewById(R.id.eth_gw_edit);
        IPView Dns1 = (IPView)mView.findViewById(R.id.eth_dns_edit1);
        IPView Dns2 = (IPView)mView.findViewById(R.id.eth_dns_edit2);

        mRJ45StaticIpViews = new RJ45StaticIpViews(Ipaddr,NetMask,Gw,Dns1,Dns2);
        mUsbCardStaticIpViews = new UsbCardStaticIpViews(Ipaddr,NetMask,Gw,Dns1,Dns2);

        String ifaceType = getInterfaceSetting();   
        if(ifaceType.equals(Settings.Global.INTERFACE_ETH0)){
            mStaticIpViews = mRJ45StaticIpViews;
        }else{
            mStaticIpViews = mUsbCardStaticIpViews;
        }
        mStaticIpViews.disableAllViews();
        
        setButton(BUTTON_POSITIVE, mContext.getText(R.string.menu_save), (DialogInterface.OnClickListener)null);
        setButton(BUTTON_NEGATIVE, mContext.getText(R.string.menu_cancel), this);

        updateRadioButtonState();

        setInverseBackgroundForced(true);

       
        eth0Radio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                Log.i("eth0Radio","onCheckedChanged isChecked:"+isChecked);
                if(isChecked){
                    mStaticIpViews = mRJ45StaticIpViews;
                    
                    setInterfaceSetting(Settings.Global.INTERFACE_ETH0);

                    updateIpTypeRadioButtonState();
                    mStaticIpViews.clearAllViews();
                    mStaticIpViews.loadSettings();
                }
            }
        });
        
        eth1Radio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                Log.i("eth1Radio","onCheckedChanged isChecked:"+isChecked);
                if(isChecked){
                    mStaticIpViews = mUsbCardStaticIpViews;
                    
                    setInterfaceSetting(Settings.Global.INTERFACE_ETH1);

                    updateIpTypeRadioButtonState();
                    mStaticIpViews.clearAllViews();
                    mStaticIpViews.loadSettings();
                }
            }
        });

        mIpTypeDhcp.setOnClickListener(new RadioButton.OnClickListener() {
            public void onClick(View v) {
                mStaticIpViews.setIpTypeSetting(Settings.Global.TYPE_DHCP);

                mStaticIpViews.disableAllViews();
                mStaticIpViews.clearAllViews();
            }
        });
        
        mIpTypeManual.setOnClickListener(new RadioButton.OnClickListener() {
            public void onClick(View v) {
                mStaticIpViews.setIpTypeSetting(Settings.Global.TYPE_STATIC);

                mStaticIpViews.enableAllViews();
                mStaticIpViews.clearAllViews();
                mStaticIpViews.loadSettings();
            }
        });

        UpdateViewContent();

    }

    private void UpdateViewContent() {
        int enable = Global.getInt(mContext.getContentResolver(), Global.ETHERNET_ON, EthernetManager.ETHERNET_STATE_UNKNOWN);
        if (localLOGV) Log.d(TAG, "UpdateViewContent enable=" + enable);
        
        if(enable == EthernetManager.ETHERNET_STATE_ENABLED) {
            String ifaceType = getInterfaceSetting();    

            IpConfiguration ipinfo = mEthManager.getConfiguration(ifaceType);

            if((ipinfo.ipAssignment == IpAssignment.DHCP) ||(ipinfo.ipAssignment == IpAssignment.UNASSIGNED) ) {
                    mIpTypeDhcp.setChecked(true);
                    mStaticIpViews.disableAllViews();
                    mStaticIpViews.clearAllViews();
            } else {
                    mIpTypeManual.setChecked(true);
                    mStaticIpViews.enableAllViews();
               
                    StaticIpConfiguration staticConfig = ipinfo.getStaticIpConfiguration();
                    Log.d(TAG, "UpdateViewContent staticConfig=" + staticConfig);

                    mStaticIpViews.updateViews(staticConfig);
            }
        }
    }

    private String updateIfaceTypeRadioButtonState(){
        String ifaceType = getInterfaceSetting();    
        if(ifaceType.equals(Settings.Global.INTERFACE_ETH0)){
            eth0Radio.setChecked(true);
        }else if(ifaceType.equals(Settings.Global.INTERFACE_ETH1)){
            eth1Radio.setChecked(true);
        }
        return ifaceType;
    }

    private String updateIpTypeRadioButtonState(){
        String ipType = mStaticIpViews.getIpTypeSetting();
        if(ipType.equals(Settings.Global.TYPE_DHCP)){
            mIpTypeDhcp.setChecked(true);
            mIpTypeManual.setChecked(false);
        }else if(ipType.equals(Settings.Global.TYPE_STATIC)){
            mIpTypeDhcp.setChecked(false);
            mIpTypeManual.setChecked(true);
        }
        
        return ipType;
    }
    private void updateRadioButtonState(){
        updateIfaceTypeRadioButtonState();

        updateIpTypeRadioButtonState();
    }

    private String getInterfaceSetting(){
        String ifaceType = Settings.Global.getString(mContext.getContentResolver(), Settings.Global.ETHERNET_IFACE_TYPE);
        
        return ifaceType;
    }

    private boolean setInterfaceSetting(String iface){
        return Settings.Global.putString(mContext.getContentResolver(), Settings.Global.ETHERNET_IFACE_TYPE, iface);

    }

    private void handle_saveconf() {
        IpAssignment ipAssignment = (mIpTypeDhcp.isChecked() ? IpAssignment.DHCP:IpAssignment.STATIC);

        String iface;
        if(eth0Radio.isChecked()){
            iface = Settings.Global.INTERFACE_ETH0;
        }else{
            iface = Settings.Global.INTERFACE_ETH1;
        }

        if(IpAssignment.DHCP == ipAssignment){
            mEthManager.setConfiguration(iface,new IpConfiguration(ipAssignment, ProxySettings.NONE,
                                null, null));
            dismiss();
        }else{
            StaticIpConfiguration staticIpConfiguration = mStaticIpViews.generateStaticIpConfiguration();

            if(staticIpConfiguration == null){
                Toast.makeText(mContext, R.string.eth_settings_error, Toast.LENGTH_SHORT).show();
            }else{
                mEthManager.setConfiguration(iface,new IpConfiguration(ipAssignment, ProxySettings.NONE,
                                                staticIpConfiguration, null));
                Log.d(TAG,"mode static ip ++ staticIpConfiguration=" + staticIpConfiguration);
            
                mStaticIpViews.saveSettings(); 
                dismiss();
            }
            
        }
 
    }


    private abstract class StaticIpViews{
        private final String TAG = "StaticIpViews";
        
        protected IPView mIpaddr;
        protected IPView mNetMask;
        protected IPView mGw;
        protected IPView mDns1;
        protected IPView mDns2;
        
        
        public StaticIpViews(IPView ipaddr, IPView netMask, IPView gateway, IPView dns1, IPView dns2){
            mIpaddr = ipaddr;
            mNetMask = netMask;
            mGw = gateway;
            mDns1 = dns1;
            mDns2 = dns2;
        }

        public void updateViews(StaticIpConfiguration staticIpConfig ){
            if (staticIpConfig != null) {
                if (staticIpConfig.ipAddress != null) {
                    mIpaddr.setIp(staticIpConfig.ipAddress.getAddress().getHostAddress());
                                
                    String prefix=interMask2String(staticIpConfig.ipAddress.getPrefixLength());
                    Log.d(TAG, "updateViews prefix=" + prefix);
                    mNetMask.setIp(prefix);
                }
                        
                if (staticIpConfig.gateway != null) {
                     mGw.setIp(staticIpConfig.gateway.getHostAddress());
                }

                Iterator<InetAddress> dnsIterator = staticIpConfig.dnsServers.iterator();
                if(dnsIterator.hasNext()){
                    InetAddress ia = dnsIterator.next();
                    mDns1.setIp(ia.getHostAddress());
                }
                if(dnsIterator.hasNext()){
                    InetAddress ia = dnsIterator.next();
                    mDns2.setIp(ia.getHostAddress());
                }

            }
        }

        public void enableAllViews(){
            setAllViewsEnabled(true);
        }

        public void disableAllViews(){
            setAllViewsEnabled(false);
        }
        
        private void setAllViewsEnabled(boolean enabled){
            mIpaddr.setEnabled(enabled);
            mNetMask.setEnabled(enabled);
            mGw.setEnabled(enabled);
            mDns1.setEnabled(enabled);
            mDns2.setEnabled(enabled);
        }

        public void clearAllViews(){
            mIpaddr.clear();
            mNetMask.clear();
            mGw.clear();
            mDns1.clear();
            mDns2.clear();
        }

        public abstract  void saveSettings();

        public abstract void loadSettings();

        public abstract String getIpTypeSetting();

        public abstract boolean setIpTypeSetting(String ipType);

        public void readSettingsToIPView(IPView ipv, String settingString){
            String data =  Global.getString(mContext.getContentResolver(), settingString);
            if(data != null && isIpAddress(data)) {
                ipv.setIp(data);
            }
        }

        public StaticIpConfiguration generateStaticIpConfiguration(){

            String ipaddr = mIpaddr.getText();
            String netMask = mNetMask.getText();
            String gateway = mGw.getText();
            String dns1 = mDns1.getText();
            String dns2 = mDns2.getText();

            InvalidInputHandler invalidInputHandler = new InvalidInputHandler();
            StaticIpConfiguration staticIpConfiguration = new StaticIpConfiguration();
            
            if (TextUtils.isEmpty(ipaddr)) {
                invalidInputHandler.addResult(ConfigErrorCodeEnum.CONFIG_STR_EMPTY);
            }else{
                Inet4Address inetAddr = getIPv4Address(ipaddr);
                
                if (inetAddr == null || inetAddr.equals(Inet4Address.ANY)) {
                    invalidInputHandler.addResult(ConfigErrorCodeEnum.CONFIG_INVAILD_IP);
                }else{

                    int networkPrefixLength = -1;
                    try {
                        networkPrefixLength = maskStr2InetMask(netMask); 
                        staticIpConfiguration.ipAddress = new LinkAddress(inetAddr, networkPrefixLength);
                    } catch (NumberFormatException e) {
                        // Set the hint as default after user types in ip address
                        invalidInputHandler.addResult(ConfigErrorCodeEnum.CONFI_ERROR);
                    } catch (IllegalArgumentException e) {
                        invalidInputHandler.addResult(ConfigErrorCodeEnum.CONFI_ERROR);
                    }
                }
            }
        
            if (!TextUtils.isEmpty(gateway)) {
                InetAddress gatewayAddr = getIPv4Address(gateway);
                if (gatewayAddr == null) {
                    invalidInputHandler.addResult(ConfigErrorCodeEnum.CONFIG_INVAILD_GATEWAY);
                }
                if (gatewayAddr.isMulticastAddress()) {
                    invalidInputHandler.addResult(ConfigErrorCodeEnum.CONFIG_INVAILD_GATEWAY);
                }
                staticIpConfiguration.gateway = gatewayAddr;
            }

            InetAddress dnsAddr = null;
            if (dns1 != null && !TextUtils.isEmpty(dns1)) {
                dnsAddr = getIPv4Address(dns1);
                if (dnsAddr == null) {
                    invalidInputHandler.addResult(ConfigErrorCodeEnum.CONFIG_INVAILD_DNS);
                }
                staticIpConfiguration.dnsServers.add(dnsAddr);
            }

            if (dns2 != null && !TextUtils.isEmpty(dns2)) {
                dnsAddr = getIPv4Address(dns2);
                if (dnsAddr == null) {
                    invalidInputHandler.addResult(ConfigErrorCodeEnum.CONFIG_INVAILD_DNS);
                }
                staticIpConfiguration.dnsServers.add(dnsAddr);
            }

            Log.d(TAG, "generateStaticIpConfiguration result=" + invalidInputHandler.getResults());
            if(0 == invalidInputHandler.getResults()){
                return staticIpConfiguration;
            }else{               
                return null;
            }
        }
        
        public  String interMask2String(int prefixLength) {
        String netMask = null;
		int inetMask = prefixLength;
		
		int part = inetMask / 8;
		int remainder = inetMask % 8;
		int sum = 0;
		
		for (int i = 8; i > 8 - remainder; i--) {
			sum = sum + (int) Math.pow(2, i - 1);
		}
		
		if (part == 0) {
			netMask = sum + ".0.0.0";
		} else if (part == 1) {
			netMask = "255." + sum + ".0.0";
		} else if (part == 2) {
			netMask = "255.255." + sum + ".0";
		} else if (part == 3) {
			netMask = "255.255.255." + sum;
		} else if (part == 4) {
			netMask = "255.255.255.255";
		}

		return netMask;
	}

   /*
     * convert subMask string to prefix length
     */
       public int maskStr2InetMask(String maskStr) {
    	StringBuffer sb ;
    	String str;
    	int inetmask = 0; 
    	int count = 0;
    	/*
    	 * check the subMask format
    	 */
      	Pattern pattern = Pattern.compile("(^((\\d|[01]?\\d\\d|2[0-4]\\d|25[0-5])\\.){3}(\\d|[01]?\\d\\d|2[0-4]\\d|25[0-5])$)|^(\\d|[1-2]\\d|3[0-2])$");
    	if (pattern.matcher(maskStr).matches() == false) {
    		Log.e(TAG,"subMask is error");
   		return 0;
    	}
    	
    	String[] ipSegment = maskStr.split("\\.");
    	for(int n =0; n<ipSegment.length;n++) {
   		sb = new StringBuffer(Integer.toBinaryString(Integer.parseInt(ipSegment[n])));
    		str = sb.reverse().toString();
    		count=0;
    		for(int i=0; i<str.length();i++) {
    			i=str.indexOf("1",i);
   			if(i==-1)  
    				break;
    			count++;
    		}
    		inetmask+=count;
    	}
    	return inetmask;
        }

        public Inet4Address getIPv4Address(String text) {
        try {
            return (Inet4Address) NetworkUtils.numericToInetAddress(text);
        } catch (IllegalArgumentException|ClassCastException e) {
            return null;
        }
        }

        public boolean isIpAddress(String value) {
        int start = 0;
        int end = value.indexOf('.');
        int numBlocks = 0;

        while (start < value.length()) {
            if (end == -1) {
                end = value.length();
            }

            try {
                int block = Integer.parseInt(value.substring(start, end));
                if ((block > 255) || (block < 0)) {
                        return false;
                }
            } catch (NumberFormatException e) {
                    return false;
            }

            numBlocks++;

            start = end + 1;
            end = value.indexOf('.', start);
        }
        return numBlocks == 4;
        }
        
    }

    private class RJ45StaticIpViews extends StaticIpViews{
        public RJ45StaticIpViews (IPView ipaddr, IPView netMask, IPView gateway, IPView dns1, IPView dns2){
            super(ipaddr,netMask,gateway,dns1,dns2);
        }

        @Override
        public void saveSettings(){
                Global.putString(mContext.getContentResolver(), Global.RJ45_STATIC_IP,
                    mIpaddr.getText());
                Global.putString(mContext.getContentResolver(), Global.RJ45_STATIC_NETMASK,
                    mNetMask.getText().toString());
                Global.putString(mContext.getContentResolver(), Global.RJ45_STATIC_GATEWAY,
                    mGw.getText());
                Global.putString(mContext.getContentResolver(), Global.RJ45_STATIC_DNS1,
                    mDns1.getText());
                Global.putString(mContext.getContentResolver(), Global.RJ45_STATIC_DNS2,
                    mDns2.getText());
        }

        @Override
        public void loadSettings(){
            String ipType = getIpTypeSetting();
            if (localLOGV) Log.d(TAG, "ipType =" + ipType );

            if(ipType.equals(Settings.Global.TYPE_STATIC)){
                readSettingsToIPView(mIpaddr,Global.RJ45_STATIC_IP);
                readSettingsToIPView(mNetMask,Global.RJ45_STATIC_NETMASK);
                readSettingsToIPView(mGw,Global.RJ45_STATIC_GATEWAY);
                readSettingsToIPView(mDns1,Global.RJ45_STATIC_DNS1);
                readSettingsToIPView(mDns2,Global.RJ45_STATIC_DNS2);   
                enableAllViews();
            }else{
                disableAllViews();
                clearAllViews();
            }
        }

        @Override
        public String getIpTypeSetting(){
            String ipType = Settings.Global.getString(mContext.getContentResolver(), Settings.Global.ETHERNET_IP_TYPE_ETH0);

            return ipType;
        }

        @Override
        public boolean setIpTypeSetting(String ipType){
            return Settings.Global.putString(mContext.getContentResolver(), Settings.Global.ETHERNET_IP_TYPE_ETH0, ipType);
        }
        
    }

    private class UsbCardStaticIpViews extends StaticIpViews{
        public UsbCardStaticIpViews (IPView ipaddr, IPView netMask, IPView gateway, IPView dns1, IPView dns2){
            super(ipaddr,netMask,gateway,dns1,dns2);
        }

        @Override
        public void saveSettings(){
                Global.putString(mContext.getContentResolver(), Global.USB_CARD_STATIC_IP,
                    mIpaddr.getText());
                Global.putString(mContext.getContentResolver(), Global.USB_CARD_STATIC_NETMASK,
                    mNetMask.getText().toString());
                Global.putString(mContext.getContentResolver(), Global.USB_CARD_STATIC_GATEWAY,
                    mGw.getText());
                Global.putString(mContext.getContentResolver(), Global.USB_CARD_STATIC_DNS1,
                    mDns1.getText());
                Global.putString(mContext.getContentResolver(), Global.USB_CARD_STATIC_DNS2,
                    mDns2.getText());
        }

        @Override
        public void loadSettings(){
            String ipType = getIpTypeSetting();
            if (localLOGV) Log.d(TAG, "ipType =" + ipType );

            if(ipType.equals(Settings.Global.TYPE_STATIC)){
                readSettingsToIPView(mIpaddr,Global.USB_CARD_STATIC_IP);
                readSettingsToIPView(mNetMask,Global.USB_CARD_STATIC_NETMASK);
                readSettingsToIPView(mGw,Global.USB_CARD_STATIC_GATEWAY);
                readSettingsToIPView(mDns1,Global.USB_CARD_STATIC_DNS1);
                readSettingsToIPView(mDns2,Global.USB_CARD_STATIC_DNS2); 
                enableAllViews();
            }else{
                disableAllViews();
                clearAllViews();
            }
            
        }

        @Override
        public String getIpTypeSetting(){
            String ipType = Settings.Global.getString(mContext.getContentResolver(), Settings.Global.ETHERNET_IP_TYPE_ETH1);

            return ipType;
        }

        @Override
        public boolean setIpTypeSetting(String ipType){
            return Settings.Global.putString(mContext.getContentResolver(), Settings.Global.ETHERNET_IP_TYPE_ETH1, ipType);
        }
    }

    private class InvalidInputHandler{
        
        public static final int STR_EMPTY_MASK = ~0x01;
        private int mResults;
        
        public InvalidInputHandler(){
            mResults = ConfigErrorCodeEnum.CONFIG_OK.getCode();
        }

        public void addResult(ConfigErrorCodeEnum resultEnum){
            mResults |=resultEnum.getCode();
        }

        public int getResults(){
            return mResults;
        }
        
        public void popupIndication(){
            
        }
        
    }


    public static enum ConfigErrorCodeEnum{
            CONFIG_OK(0,0),
            CONFIG_STR_EMPTY(0x01, R.string.eth_settings_empty),
            CONFIG_INVAILD_IP(0x02, R.string.eth_settings_invalid_ip_address),
            CONFIG_INVAILD_GATEWAY(0x04, R.string.eth_settings_invalid_gateway),
            CONFIG_INVAILD_DNS(0x08, R.string.eth_settings_invalid_dns),
            CONFI_ERROR(0x10, R.string.eth_settings_error);

            private int mCode;
            private int mTextId;

            private ConfigErrorCodeEnum(int code, int textId){
                mCode = code;
                mTextId = textId;
            }

            public int getCode(){
                return mCode;
            }
            
            public static int getTextIdByCode(int code ){
                int rTextId = 0;
                for(ConfigErrorCodeEnum errorCode:values()){
                    if(errorCode.mCode == code){
                        rTextId = errorCode.mTextId;
                        break;
                    }
                }

                return rTextId;
            }
    }

    
}
