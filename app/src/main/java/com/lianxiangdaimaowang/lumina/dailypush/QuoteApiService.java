package com.lianxiangdaimaowang.lumina.dailypush;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 名人名言API服务类
 * 负责调用天行数据的名人名言API
 */
public class QuoteApiService {
    private static final String TAG = "QuoteApiService";
    
    // 天行数据API密钥
    private static final String API_KEY = "0cd1cd7f52188eb63d6bad916d8fc452";
    
    // 名人名言API端点
    private static final String QUOTE_API_URL = "https://apis.tianapi.com/mingyan/index";
    
    // OkHttpClient实例
    private final OkHttpClient httpClient;
    
    // 名言数据模型
    public static class QuoteData {
        private String content;
        private String author;
        private int typeId;
        private String typeTitle;

        public QuoteData(String content, String author, int typeId, String typeTitle) {
            this.content = content;
            this.author = author;
            this.typeId = typeId;
            this.typeTitle = typeTitle;
        }

        public String getContent() {
            return content;
        }

        public String getAuthor() {
            return author;
        }

        public int getTypeId() {
            return typeId;
        }

        public String getTypeTitle() {
            return typeTitle;
        }
        
        public String getFormattedContent() {
            return content + "\n\n—— " + author;
        }
    }
    
    // 单例模式
    private static QuoteApiService instance;
    
    private QuoteApiService() {
        httpClient = new OkHttpClient();
    }
    
    public static synchronized QuoteApiService getInstance() {
        if (instance == null) {
            instance = new QuoteApiService();
        }
        return instance;
    }
    
    /**
     * 获取随机名人名言
     * @param typeId 名言类型ID
     * @param callback 回调接口
     */
    public void getRandomQuote(int typeId, TianApiManager.ApiCallback<QuoteData> callback) {
        // 构建API URL
        String url = QUOTE_API_URL + "?key=" + API_KEY + "&num=1&typeid=" + typeId;
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API请求失败", e);
                if (callback != null) {
                    callback.onError("网络请求失败: " + e.getMessage());
                }
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    if (callback != null) {
                        callback.onError("服务器响应错误: " + response.code());
                    }
                    return;
                }
                
                try {
                    String responseData = response.body().string();
                    JSONObject jsonObject = new JSONObject(responseData);
                    
                    // 解析API响应
                    if (jsonObject.getInt("code") == 200) {
                        JSONObject resultObj = jsonObject.getJSONObject("result");
                        JSONArray list = resultObj.getJSONArray("list");
                        
                        if (list.length() > 0) {
                            JSONObject quoteData = list.getJSONObject(0);
                            
                            String content = quoteData.getString("content");
                            String author = quoteData.getString("author");
                            int resultTypeId = quoteData.optInt("typeid", typeId);
                            
                            // 获取类型标题
                            String typeTitle = TianApiManager.getInstance().getQuoteTypeTitle(resultTypeId);
                            
                            QuoteData data = new QuoteData(content, author, resultTypeId, typeTitle);
                            
                            if (callback != null) {
                                callback.onSuccess(data);
                            }
                        } else {
                            if (callback != null) {
                                callback.onError("没有找到名人名言数据");
                            }
                        }
                    } else {
                        String errorMsg = jsonObject.optString("msg", "未知错误");
                        if (callback != null) {
                            callback.onError("API返回错误: " + errorMsg);
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "解析JSON失败", e);
                    if (callback != null) {
                        callback.onError("数据解析失败: " + e.getMessage());
                    }
                }
            }
        });
    }
    
    /**
     * 获取内置名言数据（当API调用失败时使用）
     * @return 随机生成的内置名言数据
     */
    public QuoteData getFallbackQuote() {
        // 内置名言数据
        String[][] quotes = {
            // 中国古代名言
            {"不积跬步，无以至千里；不积小流，无以成江海。", "荀子", "《劝学》"},
            {"天行健，君子以自强不息。", "《周易》", "中国古代"},
            {"路漫漫其修远兮，吾将上下而求索。", "屈原", "《离骚》"},
            {"桃花潭水深千尺，不及汪伦送我情。", "李白", "《赠汪伦》"},
            {"人生自古谁无死，留取丹心照汗青。", "文天祥", "《过零丁洋》"},
            {"海内存知己，天涯若比邻。", "王勃", "《送杜少府之任蜀州》"},
            {"滚滚长江东逝水，浪花淘尽英雄。", "《三国演义》", "中国古代"},
            {"欲穷千里目，更上一层楼。", "王之涣", "《登鹳雀楼》"},
            {"千磨万击还坚劲，任尔东西南北风。", "郑板桥", "《竹石》"},
            {"采菊东篱下，悠然见南山。", "陶渊明", "《饮酒》"},
            
            // 中国现代名言
            {"愿中国青年都摆脱冷气，只是向上走，不必听自暴自弃者流的话。", "鲁迅", "《热风》"},
            {"人最宝贵的东西是生命，生命对人来说只有一次。", "奥斯特洛夫斯基", "《钢铁是怎样炼成的》"},
            {"我所学到的任何有价值的知识都是由自学中得来的。", "达尔文", "中国现代"},
            {"伟大的事业，需要决心，能力，组织和责任感。", "毛泽东", "中国现代"},
            {"世上无难事，只要肯登攀。", "毛泽东", "中国现代"},
            {"人的一生可能燃烧也可能腐朽，我不能腐朽，我愿意燃烧起来！", "奥斯特洛夫斯基", "《钢铁是怎样炼成的》"},
            {"天才就是百分之九十九的汗水加百分之一的灵感。", "爱迪生", "中国现代"},
            {"人的天职在勇于探索真理。", "哥白尼", "中国现代"},
            {"读书破万卷，下笔如有神。", "杜甫", "中国现代"},
            {"理想的书籍是智慧的钥匙。", "列夫·托尔斯泰", "中国现代"},
            
            // 人生哲理
            {"人生就像一场旅行，不必在乎目的地，在乎的是沿途的风景以及看风景的心情。", "未知", "人生哲理"},
            {"生活不是单行线，一条路走不通，你可以转弯。", "未知", "人生哲理"},
            {"简单的事情重复做，你就是专家；重复的事情用心做，你就是赢家。", "未知", "人生哲理"},
            {"没有退路时，潜能就发挥出来了。", "未知", "人生哲理"},
            {"宁可做过了后悔，也不要错过了后悔。", "未知", "人生哲理"},
            {"输给自己是成长，输给别人是失败。", "未知", "人生哲理"},
            {"如果你不给自己烦恼，别人也不可能给你烦恼，烦恼都是自己内心制造的。", "未知", "人生哲理"},
            {"不要等待机会，而要创造机会。", "未知", "人生哲理"},
            {"目标的坚定是性格中最必要的力量源泉之一，也是成功的利器之一。", "未知", "人生哲理"},
            {"不要因为结束而哭泣，微笑吧，为你的曾经拥有。", "未知", "人生哲理"},
            
            // 励志名言
            {"世上没有绝望的处境，只有对处境绝望的人。", "未知", "励志名言"},
            {"只有一条路不能选择——那就是放弃的路；只有一条路不能拒绝——那就是成长的路。", "未知", "励志名言"},
            {"再长的路，一步步也能走完，再短的路，不迈开双脚也无法到达。", "未知", "励志名言"},
            {"要克服生活的焦虑和沮丧，得先学会做自己的主人。", "未知", "励志名言"},
            {"你不能左右天气，但你能转变你的心情。", "未知", "励志名言"},
            {"凡事先难后易，先急后缓，先做后说，先苦后甜。", "未知", "励志名言"},
            {"种子牢记着雨滴献身的叮嘱，增强了冒尖的勇气。", "未知", "励志名言"},
            {"如果你曾歌颂黎明，那么也请你拥抱黑夜。", "纪伯伦", "励志名言"},
            {"跌倒了爬起来再哭，哭完了继续走。", "未知", "励志名言"},
            {"成功不是将来才有的，而是从决定去做的那一刻起，持续累积而成。", "未知", "励志名言"},
            
            // 友情名言
            {"友谊是一株开花的树，需要用真诚培育和浇灌。", "未知", "友情名言"},
            {"朋友是一面镜子，照出你最真实的自己。", "未知", "友情名言"},
            {"真正的朋友不是在你成功时来贺喜的，而是在你失意时不离不弃的。", "未知", "友情名言"},
            {"友谊是一种和谐的平等，既不卑躬屈膝，也不趾高气扬。", "未知", "友情名言"},
            {"友谊是灵魂和灵魂的结合，是心与心的沟通。", "未知", "友情名言"},
            {"朋友之间最大的信任就是不设防。", "未知", "友情名言"},
            {"友谊就像清晨的雾一样纯洁，奉承就像午后的云一般虚幻。", "未知", "友情名言"},
            {"真正的友谊不是一种处世手段，而是心灵的相知，情感的相融。", "未知", "友情名言"},
            {"真正的友谊不是你痛苦时想起的人，而是你痛苦时想起你的人。", "未知", "友情名言"},
            {"朋友是路，家是树，你在树下休息，然后上路。", "未知", "友情名言"},
            
            // 爱情名言
            {"爱情是灵魂的一种延伸，让我们一个人的身体里，装下两个人的梦想。", "未知", "爱情名言"},
            {"爱情是一种信仰，不要问为什么，不要问值不值得。", "未知", "爱情名言"},
            {"爱情是一种需要慢慢培养的情感，它需要理解、包容和信任。", "未知", "爱情名言"},
            {"爱情不是寻找一个完美的人，而是学会用完美的眼光，欣赏一个不完美的人。", "未知", "爱情名言"},
            {"爱情是一个人对另一个人全部的理解，包括他的缺点。", "未知", "爱情名言"},
            {"真正的爱情不是一时的好感，而是永远的责任。", "未知", "爱情名言"},
            {"爱情不是彼此凝视，而是一起朝着同一个方向看。", "圣埃克苏佩里", "爱情名言"},
            {"世界上最远的距离不是生与死，而是我站在你面前，你却不知道我爱你。", "未知", "爱情名言"},
            {"爱情是一种甜蜜的痛苦，是一种痛苦的甜蜜。", "未知", "爱情名言"},
            {"爱情就像种子，需要信任才能生根，需要关怀才能茁壮。", "未知", "爱情名言"}
        };
        
        // 随机选择一条名言
        int randomIndex = new Random().nextInt(quotes.length);
        String[] quote = quotes[randomIndex];
        
        return new QuoteData(quote[0], quote[1], 0, quote[2]);
    }
} 