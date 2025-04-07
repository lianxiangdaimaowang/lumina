package com.lianxiangdaimaowang.lumina.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

/**
 * 网络管理器，用于检查网络状态和连接
 */
public class NetworkManager {
    private static final String TAG = "NetworkManager";
    
    // 单例实例
    private static NetworkManager instance;
    
    // 上下文
    private Context context;
    
    // 连接管理器
    private ConnectivityManager connectivityManager;
    
    /**
     * 获取NetworkManager的单例实例
     * @param context 上下文
     * @return NetworkManager实例
     */
    public static synchronized NetworkManager getInstance(Context context) {
        try {
            if (instance == null) {
                Log.d(TAG, "创建新的NetworkManager实例");
                instance = new NetworkManager(context);
            }
            return instance;
        } catch (Exception e) {
            Log.e(TAG, "NetworkManager.getInstance失败", e);
            return new NetworkManager(context); // 返回一个新实例而不是null
        }
    }
    
    /**
     * 构造函数
     * @param context 上下文
     */
    private NetworkManager(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }
    
    /**
     * 检查是否有活跃的网络连接
     * @return 如果有网络连接返回true，否则返回false
     */
    public boolean isNetworkConnected() {
        try {
            if (connectivityManager == null) {
                Log.e(TAG, "isNetworkConnected: ConnectivityManager为空");
                return false;
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6.0及以上版本
                Network network = connectivityManager.getActiveNetwork();
                if (network == null) {
                    Log.d(TAG, "isNetworkConnected: 没有活跃的网络");
                    return false;
                }
                
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                if (capabilities == null) {
                    Log.d(TAG, "isNetworkConnected: 无法获取网络能力");
                    return false;
                }
                
                // 检查各种网络类型
                boolean hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                     capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                
                if (hasInternet) {
                    Log.d(TAG, "isNetworkConnected: 有可用的互联网连接");
                    return true;
                } else {
                    Log.d(TAG, "isNetworkConnected: 没有互联网连接能力");
                    return false;
                }
            } else {
                // 兼容Android 6.0以下版本
                NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
                
                if (isConnected) {
                    Log.d(TAG, "isNetworkConnected: 有活跃的网络连接");
                } else {
                    Log.d(TAG, "isNetworkConnected: 没有活跃的网络连接");
                }
                
                return isConnected;
            }
        } catch (Exception e) {
            Log.e(TAG, "isNetworkConnected: 检查网络连接失败", e);
            return false;
        }
    }
    
    /**
     * 获取网络类型
     * @return 网络类型描述字符串
     */
    public String getNetworkType() {
        try {
            if (connectivityManager == null) {
                return "未知";
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network == null) {
                    return "无网络";
                }
                
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                if (capabilities == null) {
                    return "未知";
                }
                
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return "WiFi";
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return "移动数据";
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    return "以太网";
                } else {
                    return "其他";
                }
            } else {
                NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                if (activeNetwork == null) {
                    return "无网络";
                }
                
                int type = activeNetwork.getType();
                if (type == ConnectivityManager.TYPE_WIFI) {
                    return "WiFi";
                } else if (type == ConnectivityManager.TYPE_MOBILE) {
                    return "移动数据";
                } else if (type == ConnectivityManager.TYPE_ETHERNET) {
                    return "以太网";
                } else {
                    return "其他";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getNetworkType: 获取网络类型失败", e);
            return "未知";
        }
    }
    
    /**
     * 检查是否已连接到WiFi
     * @return 如果已连接到WiFi返回true，否则返回false
     */
    public boolean isWifiConnected() {
        try {
            if (connectivityManager == null) {
                return false;
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network == null) {
                    return false;
                }
                
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            } else {
                NetworkInfo wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                return wifiInfo != null && wifiInfo.isConnected();
            }
        } catch (Exception e) {
            Log.e(TAG, "isWifiConnected: 检查WiFi连接失败", e);
            return false;
        }
    }
    
    /**
     * 检查是否已连接到移动数据网络
     * @return 如果已连接到移动数据网络返回true，否则返回false
     */
    public boolean isMobileConnected() {
        try {
            if (connectivityManager == null) {
                return false;
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network == null) {
                    return false;
                }
                
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            } else {
                NetworkInfo mobileInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                return mobileInfo != null && mobileInfo.isConnected();
            }
        } catch (Exception e) {
            Log.e(TAG, "isMobileConnected: 检查移动数据连接失败", e);
            return false;
        }
    }
    
    /**
     * 诊断网络连接，主要检测科大讯飞API的连通性
     * @return 诊断结果
     */
    public String diagnoseNetworkConnection() {
        StringBuilder result = new StringBuilder();
        try {
            // 检查网络连接基本状态
            boolean isConnected = isNetworkConnected();
            result.append("网络连接状态: ").append(isConnected ? "已连接" : "未连接").append("\n");
            result.append("网络类型: ").append(getNetworkType()).append("\n");
            
            // 检查DNS解析
            Runtime runtime = Runtime.getRuntime();
            try {
                Process process = runtime.exec("ping -c 1 iat-api.xfyun.cn");
                int exitValue = process.waitFor();
                result.append("DNS解析: ").append(exitValue == 0 ? "正常" : "异常").append("\n");
            } catch (Exception e) {
                result.append("DNS解析: 异常 (").append(e.getMessage()).append(")\n");
            }
            
            // 检查HTTPS连接
            try {
                java.net.URL url = new java.net.URL("https://iat-api.xfyun.cn");
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestMethod("HEAD");
                int responseCode = connection.getResponseCode();
                result.append("HTTPS连接: ").append(responseCode >= 200 && responseCode < 400 ? "正常" : "异常").append("\n");
            } catch (Exception e) {
                result.append("HTTPS连接: 异常 (").append(e.getMessage()).append(")\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            Log.e(TAG, "诊断网络连接失败", e);
            return "网络诊断失败: " + e.getMessage();
        }
    }
    
    /**
     * 检查特定域名的连通性
     * @param domain 要检查的域名
     * @return 如果能连通返回true，否则返回false
     */
    public boolean checkDomainConnectivity(String domain) {
        try {
            java.net.URL url = new java.net.URL("https://" + domain);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            return responseCode >= 200 && responseCode < 400;
        } catch (Exception e) {
            Log.e(TAG, "检查域名连通性失败: " + domain, e);
            return false;
        }
    }
} 