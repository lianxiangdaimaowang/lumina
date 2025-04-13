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
 * 诗词API服务类
 * 负责获取英文经典诗词数据
 */
public class PoemApiService {
    private static final String TAG = "PoemApiService";
    
    // PoetryDB API端点
    private static final String POETRYDB_API_URL = "https://poetrydb.org";
    
    // 著名诗人列表
    private static final String[] FAMOUS_POETS = {
        "Shakespeare", "Emily Dickinson", "Wordsworth", "Percy Bysshe Shelley",
        "John Keats", "Walt Whitman", "Edgar Allan Poe", "Robert Frost",
        "William Blake", "Lord Byron"
    };
    
    // 著名诗歌标题列表
    private static final String[] FAMOUS_POEMS = {
        "Ozymandias", "The Raven", "Fire and Ice", "The Road Not Taken",
        "Hope is the thing with feathers", "Because I could not stop for Death",
        "Sonnet 18", "The Tyger", "I wandered lonely as a cloud", "O Captain! My Captain!"
    };
    
    // OkHttpClient实例
    private final OkHttpClient httpClient;
    
    // 诗词数据模型
    public static class PoemData {
        private String title;
        private String content;
        private String author;
        private String lineCount;
        
        public PoemData(String title, String content, String author, String lineCount) {
            this.title = title;
            this.content = content;
            this.author = author;
            this.lineCount = lineCount;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getContent() {
            return content;
        }
        
        public String getAuthor() {
            return author;
        }
        
        public String getLineCount() {
            return lineCount;
        }
        
        public String getFormattedContent() {
            // 英文诗歌内容已经包含换行，只需添加作者信息
            return content + "\n\n— " + author;
        }
    }
    
    // 单例模式
    private static PoemApiService instance;
    
    private PoemApiService() {
        httpClient = new OkHttpClient();
    }
    
    public static synchronized PoemApiService getInstance() {
        if (instance == null) {
            instance = new PoemApiService();
        }
        return instance;
    }
    
    /**
     * 获取随机诗词
     * @param callback 回调接口
     */
    public void getRandomPoem(TianApiManager.ApiCallback<PoemData> callback) {
        // 随机选择查询方式：按诗人或按标题
        boolean queryByAuthor = new Random().nextBoolean();
        
        String url;
        if (queryByAuthor) {
            // 随机选择一位诗人
            String poet = FAMOUS_POETS[new Random().nextInt(FAMOUS_POETS.length)];
            url = POETRYDB_API_URL + "/author/" + poet + "/title,author,lines,linecount";
        } else {
            // 随机选择一首诗
            String poem = FAMOUS_POEMS[new Random().nextInt(FAMOUS_POEMS.length)];
            url = POETRYDB_API_URL + "/title/" + poem + "/title,author,lines,linecount";
        }
        
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
                    JSONArray jsonArray = new JSONArray(responseData);
                    
                    // 解析API响应
                    if (jsonArray.length() > 0) {
                        // 如果返回多首诗，随机选择一首
                        int randomIndex = jsonArray.length() == 1 ? 0 : new Random().nextInt(jsonArray.length());
                        JSONObject poemData = jsonArray.getJSONObject(randomIndex);
                        
                        String title = poemData.getString("title");
                        JSONArray linesArray = poemData.getJSONArray("lines");
                        String author = poemData.getString("author");
                        String lineCount = poemData.getString("linecount");
                        
                        // 将诗行数组转换为带换行的字符串
                        StringBuilder contentBuilder = new StringBuilder();
                        for (int i = 0; i < linesArray.length(); i++) {
                            String line = linesArray.getString(i);
                            contentBuilder.append(line);
                            if (i < linesArray.length() - 1) {
                                contentBuilder.append("\n");
                            }
                        }
                        String content = contentBuilder.toString();
                        
                        PoemData poem = new PoemData(title, content, author, lineCount);
                        
                        if (callback != null) {
                            callback.onSuccess(poem);
                        }
                    } else {
                        if (callback != null) {
                            callback.onError("没有找到诗词数据");
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
     * 获取指定标题的诗歌
     * @param title 诗歌标题
     * @param callback 回调接口
     */
    public void getPoemByTitle(String title, TianApiManager.ApiCallback<PoemData> callback) {
        String url = POETRYDB_API_URL + "/title/" + title + "/title,author,lines,linecount";
        
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
                    JSONArray jsonArray = new JSONArray(responseData);
                    
                    // 解析API响应
                    if (jsonArray.length() > 0) {
                        JSONObject poemData = jsonArray.getJSONObject(0);
                        
                        String poemTitle = poemData.getString("title");
                        JSONArray linesArray = poemData.getJSONArray("lines");
                        String author = poemData.getString("author");
                        String lineCount = poemData.getString("linecount");
                        
                        // 将诗行数组转换为带换行的字符串
                        StringBuilder contentBuilder = new StringBuilder();
                        for (int i = 0; i < linesArray.length(); i++) {
                            String line = linesArray.getString(i);
                            contentBuilder.append(line);
                            if (i < linesArray.length() - 1) {
                                contentBuilder.append("\n");
                            }
                        }
                        String content = contentBuilder.toString();
                        
                        PoemData poem = new PoemData(poemTitle, content, author, lineCount);
                        
                        if (callback != null) {
                            callback.onSuccess(poem);
                        }
                    } else {
                        if (callback != null) {
                            callback.onError("没有找到相关诗词");
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
     * 获取内置诗词数据（当API调用失败时使用）
     * @return 随机生成的内置诗词数据
     */
    public PoemData getFallbackPoem() {
        // 内置诗词数据，包含唐诗、宋词、现代诗等不同类型
        String[][] poems = {
            // 唐诗
            {"静夜思", "李白", "唐代", "床前明月光，\n疑是地上霜。\n举头望明月，\n低头思故乡。"},
            {"登鹳雀楼", "王之涣", "唐代", "白日依山尽，\n黄河入海流。\n欲穷千里目，\n更上一层楼。"},
            {"望庐山瀑布", "李白", "唐代", "日照香炉生紫烟，\n遥看瀑布挂前川。\n飞流直下三千尺，\n疑是银河落九天。"},
            {"枫桥夜泊", "张继", "唐代", "月落乌啼霜满天，\n江枫渔火对愁眠。\n姑苏城外寒山寺，\n夜半钟声到客船。"},
            {"春晓", "孟浩然", "唐代", "春眠不觉晓，\n处处闻啼鸟。\n夜来风雨声，\n花落知多少。"},
            {"黄鹤楼送孟浩然之广陵", "李白", "唐代", "故人西辞黄鹤楼，\n烟花三月下扬州。\n孤帆远影碧空尽，\n唯见长江天际流。"},
            {"出塞", "王昌龄", "唐代", "秦时明月汉时关，\n万里长征人未还。\n但使龙城飞将在，\n不教胡马度阴山。"},
            {"送元二使安西", "王维", "唐代", "渭城朝雨浥轻尘，\n客舍青青柳色新。\n劝君更尽一杯酒，\n西出阳关无故人。"},
            
            // 宋词
            {"如梦令·昨夜雨疏风骤", "李清照", "宋代", "昨夜雨疏风骤，\n浓睡不消残酒。\n试问卷帘人，\n却道海棠依旧。\n知否，知否？\n应是绿肥红瘦。"},
            {"雨霖铃·寒蝉凄切", "柳永", "宋代", "寒蝉凄切，对长亭晚，骤雨初歇。\n都门帐饮无绪，留恋处，兰舟催发。\n执手相看泪眼，竟无语凝噎。\n念去去，千里烟波，暮霭沉沉楚天阔。\n多情自古伤离别，更那堪，冷落清秋节！\n今宵酒醒何处？杨柳岸，晓风残月。\n此去经年，应是良辰好景虚设。\n便纵有千种风情，更与何人说？"},
            {"江城子·密州出猎", "苏轼", "宋代", "老夫聊发少年狂，左牵黄，右擎苍，\n锦帽貂裘，千骑卷平冈。\n为报倾城随太守，亲射虎，看孙郎。\n酒酣胸胆尚开张，鬓微霜，又何妨？\n持节云中，何日遣冯唐？\n会挽雕弓如满月，西北望，射天狼。"},
            {"水调歌头·明月几时有", "苏轼", "宋代", "明月几时有？把酒问青天。\n不知天上宫阙，今夕是何年。\n我欲乘风归去，又恐琼楼玉宇，高处不胜寒。\n起舞弄清影，何似在人间。\n转朱阁，低绮户，照无眠。\n不应有恨，何事长向别时圆？\n人有悲欢离合，月有阴晴圆缺，此事古难全。\n但愿人长久，千里共婵娟。"},
            {"念奴娇·赤壁怀古", "苏轼", "宋代", "大江东去，浪淘尽，千古风流人物。\n故垒西边，人道是，三国周郎赤壁。\n乱石穿空，惊涛拍岸，卷起千堆雪。\n江山如画，一时多少豪杰。\n遥想公瑾当年，小乔初嫁了，雄姿英发。\n羽扇纶巾，谈笑间，樯橹灰飞烟灭。\n故国神游，多情应笑我，早生华发。\n人生如梦，一尊还酹江月。"},
            
            // 现代诗
            {"再别康桥", "徐志摩", "现代", "轻轻的我走了，\n正如我轻轻的来；\n我轻轻的招手，\n作别西天的云彩。\n那河畔的金柳，\n是夕阳中的新娘；\n波光里的艳影，\n在我的心头荡漾。\n软泥上的青荇，\n油油的在水底招摇；\n在康河的柔波里，\n我甘心做一条水草！\n那榆荫下的一潭，\n不是清泉，是天上虹；\n揉碎在浮藻间，\n沉淀着彩虹似的梦。\n寻梦？撑一支长篙，\n向青草更青处漫溯；\n满载一船星辉，\n在星辉斑斓里放歌。\n但我不能放歌，\n悄悄是别离的笙箫；\n夏虫也为我沉默，\n沉默是今晚的康桥！\n悄悄的我走了，\n正如我悄悄的来；\n我挥一挥衣袖，\n不带走一片云彩。"},
            {"面朝大海，春暖花开", "海子", "现代", "从明天起，做一个幸福的人\n喂马，劈柴，周游世界\n从明天起，关心粮食和蔬菜\n我有一所房子，面朝大海，春暖花开\n从明天起，和每一个亲人通信\n告诉他们我的幸福\n那幸福的闪电告诉我的\n我将告诉每一个人\n给每一条河每一座山取一个温暖的名字\n陌生人，我也为你祝福\n愿你有一个灿烂的前程\n愿你有情人终成眷属\n愿你在尘世获得幸福\n我只愿面朝大海，春暖花开"},
            {"雨巷", "戴望舒", "现代", "撑着油纸伞，独自\n彷徨在悠长，悠长\n又寂寥的雨巷，\n我希望逢着\n一个丁香一样地\n结着愁怨的姑娘。\n她是有\n丁香一样的颜色，\n丁香一样的芬芳，\n丁香一样的忧愁，\n在雨中哀怨，\n哀怨又彷徨。\n她彷徨在这寂寥的雨巷，\n撑着油纸伞\n像我一样，\n像我一样地\n默默彳亍着，\n冷漠，凄清，又惆怅。\n她默默地走近，\n走近，又投出\n太息一般的眼光，\n她飘过\n像梦一般地，\n像梦一般地凄婉迷茫。\n像梦中飘过\n一枝丁香地，\n我身旁飘过这女郎；\n她静默地远了，远了，\n到了颓圮的篱墙，\n走尽这雨巷。\n在雨的哀曲里，\n消了她的颜色，\n散了她的芬芳，\n消散了，甚至她的\n太息般的眼光\n丁香般的惆怅。\n撑着油纸伞，独自\n彷徨在悠长，悠长\n又寂寥的雨巷，\n我希望飘过\n一个丁香一样地\n结着愁怨的姑娘。"},
            
            // 唐宋名句
            {"登高", "杜甫", "唐代", "风急天高猿啸哀，渚清沙白鸟飞回。\n无边落木萧萧下，不尽长江滚滚来。\n万里悲秋常作客，百年多病独登台。\n艰难苦恨繁霜鬓，潦倒新停浊酒杯。"},
            {"赤壁", "杜牧", "唐代", "折戟沉沙铁未销，自将磨洗认前朝。\n东风不与周郎便，铜雀春深锁二乔。"},
            {"钱塘湖春行", "白居易", "唐代", "孤山寺北贾亭西，水面初平云脚低。\n几处早莺争暖树，谁家新燕啄春泥。\n乱花渐欲迷人眼，浅草才能没马蹄。\n最爱湖东行不足，绿杨阴里白沙堤。"},
            {"题西林壁", "苏轼", "宋代", "横看成岭侧成峰，远近高低各不同。\n不识庐山真面目，只缘身在此山中。"},
            {"饮湖上初晴后雨", "苏轼", "宋代", "水光潋滟晴方好，山色空蒙雨亦奇。\n欲把西湖比西子，淡妆浓抹总相宜。"},
            
            // 边塞诗
            {"从军行", "杨炯", "唐代", "烽火照西京，心中自不平。\n牙璋辞凤阙，铁骑绕龙城。\n雪暗凋旗画，风多杂鼓声。\n宁为百夫长，胜作一书生。"},
            {"凉州词", "王翰", "唐代", "葡萄美酒夜光杯，欲饮琵琶马上催。\n醉卧沙场君莫笑，古来征战几人回？"},
            {"关山月", "李白", "唐代", "明月出天山，苍茫云海间。\n长风几万里，吹度玉门关。\n汉下白登道，胡窥青海湾。\n由来征战地，不见有人还。\n戍客望边色，思归多苦颜。\n高楼当此夜，叹息未应闲。"}
        };
        
        // 随机选择一首诗
        int randomIndex = new Random().nextInt(poems.length);
        String[] poem = poems[randomIndex];
        
        return new PoemData(poem[0], poem[1], poem[2], poem[3]);
    }
} 