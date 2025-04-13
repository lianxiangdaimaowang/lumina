package com.lianxiangdaimaowang.lumina.dailypush;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;
import com.lianxiangdaimaowang.lumina.data.NetworkManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DailyPushFragment extends Fragment {
    private static final String TAG = "DailyPushFragment";
    private static final String LAST_UPDATE_DATE_KEY = "last_update_date";
    private static final String LAST_CONTENT_TYPE_KEY = "last_content_type";
    private static final String LAST_CONTENT_TITLE_KEY = "last_content_title";
    private static final String LAST_CONTENT_TEXT_KEY = "last_content_text";

    // UI组件
    private SwipeRefreshLayout swipeRefreshLayout;
    private CardView contentCard;
    private TextView titleText;
    private TextView dateText;
    private TextView contentText;
    private ImageView contentImage;
    private Button shareButton;
    private Button saveButton;
    private ProgressBar loadingProgress;
    private View errorView;
    private TextView errorText;
    private Button retryButton;
    
    // 测试API调用的按钮
    private Button btnTestEssay;
    private Button btnTestQuote;
    private Button btnTestPoem;
    private Button btnTestEnglish;
    private Button btnTestLocal;
    private Button btnTestError;

    // 数据管理
    private LocalDataManager localDataManager;
    private NetworkManager networkManager;
    
    // API 服务
    private EssayApiService essayApiService;
    private QuoteApiService quoteApiService;
    private PoemApiService poemApiService;
    private EnglishQuoteApiService englishQuoteApiService;
    
    // 内容类型
    private enum ContentType {
        ESSAY, QUOTE, POEM, ENGLISH_QUOTE
    }
    
    private ContentType todayContentType;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            if (getContext() != null) {
                localDataManager = LocalDataManager.getInstance(getContext());
                networkManager = NetworkManager.getInstance(getContext());
            }
            
            // 初始化所有API服务
            essayApiService = EssayApiService.getInstance();
            quoteApiService = QuoteApiService.getInstance();
            poemApiService = PoemApiService.getInstance();
            englishQuoteApiService = EnglishQuoteApiService.getInstance();
            
            // 根据日期确定今天推送的内容类型
            determineTodayContentType();
        } catch (Exception e) {
            Log.e(TAG, "onCreate: 初始化失败", e);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_daily_push, container, false);
        
        setupViews(view);
        
        // 设置下拉刷新
        swipeRefreshLayout.setOnRefreshListener(this::loadDailyContent);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorAccent,
                R.color.colorPrimaryDark
        );
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 设置重试按钮
        retryButton.setOnClickListener(v -> loadDailyContent());
        
        // 设置分享按钮
        shareButton.setOnClickListener(v -> {
            if (titleText.getText().length() > 0 && contentText.getText().length() > 0) {
                shareContent(titleText.getText().toString(), contentText.getText().toString());
            }
        });
        
        // 设置保存按钮
        saveButton.setOnClickListener(v -> {
            if (titleText.getText().length() > 0 && contentText.getText().length() > 0) {
                saveToNotes(titleText.getText().toString(), contentText.getText().toString());
            }
        });
        
        // 加载每日推送内容
        loadDailyContent();
    }
    
    private void setupViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        contentCard = view.findViewById(R.id.card_daily_content);
        titleText = view.findViewById(R.id.text_title);
        dateText = view.findViewById(R.id.text_date);
        contentText = view.findViewById(R.id.text_content);
        contentImage = view.findViewById(R.id.image_content);
        shareButton = view.findViewById(R.id.button_share);
        saveButton = view.findViewById(R.id.button_save);
        loadingProgress = view.findViewById(R.id.progress_loading);
        errorView = view.findViewById(R.id.layout_error);
        errorText = view.findViewById(R.id.text_error);
        retryButton = view.findViewById(R.id.button_retry);
        
        // 测试按钮初始化
        btnTestEssay = view.findViewById(R.id.btn_test_essay);
        btnTestQuote = view.findViewById(R.id.btn_test_quote);
        btnTestPoem = view.findViewById(R.id.btn_test_poem);
        btnTestEnglish = view.findViewById(R.id.btn_test_english);
        btnTestLocal = view.findViewById(R.id.btn_test_local);
        btnTestError = view.findViewById(R.id.btn_test_error);
        
        // 设置测试按钮点击事件
        if (btnTestEssay != null) {
            btnTestEssay.setOnClickListener(v -> {
                todayContentType = ContentType.ESSAY;
                loadDailyContent();
            });
        }
        
        if (btnTestQuote != null) {
            btnTestQuote.setOnClickListener(v -> {
                todayContentType = ContentType.QUOTE;
                loadDailyContent();
            });
        }
        
        if (btnTestPoem != null) {
            btnTestPoem.setOnClickListener(v -> {
                todayContentType = ContentType.POEM;
                loadDailyContent();
            });
        }
        
        if (btnTestEnglish != null) {
            btnTestEnglish.setOnClickListener(v -> {
                todayContentType = ContentType.ENGLISH_QUOTE;
                loadDailyContent();
            });
        }
        
        if (btnTestLocal != null) {
            btnTestLocal.setOnClickListener(v -> {
                testWithLocalData();
            });
        }
        
        if (btnTestError != null) {
            btnTestError.setOnClickListener(v -> {
                testWithError();
            });
        }
        
        // 设置当前日期
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINESE);
        dateText.setText(dateFormat.format(new Date()));
    }
    
    private void determineTodayContentType() {
        // 随机选择内容类型，而不是完全基于星期几
        Random random = new Random();
        
        // 获取当前日期信息
        Calendar calendar = Calendar.getInstance();
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);
        
        // 使用日期和年份作为随机种子，确保每天的选择是固定的
        random.setSeed((long)year * 1000 + dayOfYear);
        
        // 随机选择内容类型，但仍然考虑星期几作为权重
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int randomValue = random.nextInt(100); // 0-99的随机值
        
        // 根据星期几和随机值综合决定内容类型
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                // 周一: 60%文章, 20%名言, 10%诗词, 10%英文名言
                if (randomValue < 60) {
                    todayContentType = ContentType.ESSAY;
                } else if (randomValue < 80) {
                    todayContentType = ContentType.QUOTE;
                } else if (randomValue < 90) {
                    todayContentType = ContentType.POEM;
                } else {
                    todayContentType = ContentType.ENGLISH_QUOTE;
                }
                break;
            case Calendar.TUESDAY:
                // 周二: 20%文章, 30%名言, 40%诗词, 10%英文名言
                if (randomValue < 20) {
                    todayContentType = ContentType.ESSAY;
                } else if (randomValue < 50) {
                    todayContentType = ContentType.QUOTE;
                } else if (randomValue < 90) {
                    todayContentType = ContentType.POEM;
                } else {
                    todayContentType = ContentType.ENGLISH_QUOTE;
                }
                break;
            case Calendar.WEDNESDAY:
                // 周三: 20%文章, 50%名言, 10%诗词, 20%英文名言
                if (randomValue < 20) {
                    todayContentType = ContentType.ESSAY;
                } else if (randomValue < 70) {
                    todayContentType = ContentType.QUOTE;
                } else if (randomValue < 80) {
                    todayContentType = ContentType.POEM;
                } else {
                    todayContentType = ContentType.ENGLISH_QUOTE;
                }
                break;
            case Calendar.THURSDAY:
                // 周四: 50%文章, 20%名言, 15%诗词, 15%英文名言
                if (randomValue < 50) {
                    todayContentType = ContentType.ESSAY;
                } else if (randomValue < 70) {
                    todayContentType = ContentType.QUOTE;
                } else if (randomValue < 85) {
                    todayContentType = ContentType.POEM;
                } else {
                    todayContentType = ContentType.ENGLISH_QUOTE;
                }
                break;
            case Calendar.FRIDAY:
                // 周五: 20%文章, 20%名言, 20%诗词, 40%英文名言
                if (randomValue < 20) {
                    todayContentType = ContentType.ESSAY;
                } else if (randomValue < 40) {
                    todayContentType = ContentType.QUOTE;
                } else if (randomValue < 60) {
                    todayContentType = ContentType.POEM;
                } else {
                    todayContentType = ContentType.ENGLISH_QUOTE;
                }
                break;
            case Calendar.SATURDAY:
                // 周六: 25%文章, 15%名言, 35%诗词, 25%英文名言
                if (randomValue < 25) {
                    todayContentType = ContentType.ESSAY;
                } else if (randomValue < 40) {
                    todayContentType = ContentType.QUOTE;
                } else if (randomValue < 75) {
                    todayContentType = ContentType.POEM;
                } else {
                    todayContentType = ContentType.ENGLISH_QUOTE;
                }
                break;
            case Calendar.SUNDAY:
                // 周日: 25%文章, 25%名言, 25%诗词, 25%英文名言 (完全平均)
                if (randomValue < 25) {
                    todayContentType = ContentType.ESSAY;
                } else if (randomValue < 50) {
                    todayContentType = ContentType.QUOTE;
                } else if (randomValue < 75) {
                    todayContentType = ContentType.POEM;
                } else {
                    todayContentType = ContentType.ENGLISH_QUOTE;
                }
                break;
            default:
                // 默认情况: 完全随机
                int randomType = random.nextInt(4);
                switch (randomType) {
                    case 0:
                        todayContentType = ContentType.ESSAY;
                        break;
                    case 1:
                        todayContentType = ContentType.QUOTE;
                        break;
                    case 2:
                        todayContentType = ContentType.POEM;
                        break;
                    case 3:
                        todayContentType = ContentType.ENGLISH_QUOTE;
                        break;
                    default:
                        todayContentType = ContentType.ESSAY;
                        break;
                }
                break;
        }
        
        Log.d(TAG, "今日内容类型: " + todayContentType.name() + " (基于随机算法)");
    }
    
    private void loadDailyContent() {
        if (!isAdded()) return;

        // 获取当前日期
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        
        // 获取上次更新日期
        String lastUpdateDate = localDataManager.getValue(LAST_UPDATE_DATE_KEY, "");
        
        // 如果今天已经更新过，直接加载保存的内容
        if (currentDate.equals(lastUpdateDate)) {
            loadSavedContent();
            return;
        }
        
        showLoading(true);
        
        // 确定今天的内容类型并加载新内容
        determineTodayContentType();
        
        switch (todayContentType) {
            case ESSAY:
                loadEssayContent();
                break;
            case QUOTE:
                loadQuoteContent();
                break;
            case POEM:
                loadPoemContent();
                break;
            case ENGLISH_QUOTE:
                loadEnglishQuoteContent();
                break;
        }
    }

    private void loadSavedContent() {
        if (!isAdded()) return;
        
        String title = localDataManager.getValue(LAST_CONTENT_TITLE_KEY, "");
        String content = localDataManager.getValue(LAST_CONTENT_TEXT_KEY, "");
        
        if (!title.isEmpty() && !content.isEmpty()) {
            titleText.setText(title);
            contentText.setText(content);
            showContent();
        } else {
            // 如果没有保存的内容，重新加载
            showLoading(true);
            loadNewContent();
        }
    }

    private void loadNewContent() {
        // 获取当前日期
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        
        switch (todayContentType) {
            case ESSAY:
                loadEssayContent();
                break;
            case QUOTE:
                loadQuoteContent();
                break;
            case POEM:
                loadPoemContent();
                break;
            case ENGLISH_QUOTE:
                loadEnglishQuoteContent();
                break;
        }
        
        // 保存更新日期
        localDataManager.saveValue(LAST_UPDATE_DATE_KEY, currentDate);
    }

    private void saveCurrentContent() {
        if (!isAdded()) return;
        
        String title = titleText.getText().toString();
        String content = contentText.getText().toString();
        
        localDataManager.saveValue(LAST_CONTENT_TITLE_KEY, title);
        localDataManager.saveValue(LAST_CONTENT_TEXT_KEY, content);
        localDataManager.saveValue(LAST_CONTENT_TYPE_KEY, todayContentType.name());
    }
    
    private void loadEssayContent() {
        // 使用新方法从简书首页获取最新文章
        essayApiService.getLatestEssay(new TianApiManager.ApiCallback<EssayApiService.EssayData>() {
            @Override
            public void onSuccess(EssayApiService.EssayData result) {
                // 确保Fragment仍然附加到活动
                if (!isAdded()) return;
                
                // 使用Handler在主线程更新UI
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isAdded()) return; // 再次检查，以防在切换到主线程期间Fragment被销毁
                    
                    titleText.setText(result.getTitle());
                    contentText.setText(result.getContent() + "\n\n——" + result.getAuthor());
                    showContent();
                    
                    // 记录成功信息
                    Log.d(TAG, "成功加载文章: " + result.getTitle());
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                
                // 使用内置备用文章，而不是显示错误
                Log.d(TAG, "使用备用文章内容，API错误: " + errorMessage);
                
                // 使用Handler在主线程更新UI
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isAdded()) return; // 再次检查，以防在切换到主线程期间Fragment被销毁
                    
                    // 获取备用文章数据
                    EssayApiService.EssayData fallbackData = essayApiService.getFallbackEssay();
                    titleText.setText(fallbackData.getTitle());
                    contentText.setText(fallbackData.getContent() + "\n\n——" + fallbackData.getAuthor());
                    showContent();
                    
                    // 记录备用信息
                    Log.d(TAG, "使用备用文章: " + fallbackData.getTitle());
                });
            }
        });
    }
    
    private void loadQuoteContent() {
        // 获取随机名人名言
        int randomTypeId = TianApiManager.getInstance().getRandomQuoteTypeId();
        quoteApiService.getRandomQuote(randomTypeId, new TianApiManager.ApiCallback<QuoteApiService.QuoteData>() {
            @Override
            public void onSuccess(QuoteApiService.QuoteData result) {
                if (!isAdded()) return;
                
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isAdded()) return;
                    
                    titleText.setText(result.getTypeTitle());
                    contentText.setText(result.getFormattedContent());
                    showContent();
                    
                    Log.d(TAG, "成功加载名言: " + result.getContent());
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                
                Log.d(TAG, "使用备用名言内容，API错误: " + errorMessage);
                
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isAdded()) return;
                    
                    QuoteApiService.QuoteData fallbackData = quoteApiService.getFallbackQuote();
                    titleText.setText(fallbackData.getTypeTitle());
                    contentText.setText(fallbackData.getFormattedContent());
                    showContent();
                    
                    Log.d(TAG, "使用备用名言: " + fallbackData.getContent());
                });
            }
        });
    }
    
    private void loadPoemContent() {
        // 获取随机诗词
        poemApiService.getRandomPoem(new TianApiManager.ApiCallback<PoemApiService.PoemData>() {
            @Override
            public void onSuccess(PoemApiService.PoemData result) {
                if (!isAdded()) return;
                
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isAdded()) return;
                    
                    titleText.setText(result.getTitle());
                    contentText.setText(result.getFormattedContent());
                    showContent();
                    
                    Log.d(TAG, "成功加载诗词: " + result.getTitle());
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                
                Log.d(TAG, "使用备用诗词内容，API错误: " + errorMessage);
                
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isAdded()) return;
                    
                    PoemApiService.PoemData fallbackData = poemApiService.getFallbackPoem();
                    titleText.setText(fallbackData.getTitle());
                    contentText.setText(fallbackData.getFormattedContent());
                    showContent();
                    
                    Log.d(TAG, "使用备用诗词: " + fallbackData.getTitle());
                });
            }
        });
    }
    
    private void loadEnglishQuoteContent() {
        // 获取随机英文名言
        englishQuoteApiService.getRandomQuote(new TianApiManager.ApiCallback<EnglishQuoteApiService.EnglishQuoteData>() {
            @Override
            public void onSuccess(EnglishQuoteApiService.EnglishQuoteData result) {
                if (!isAdded()) return;
                
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isAdded()) return;
                    
                    titleText.setText("English Quote");
                    contentText.setText(result.getFormattedContent());
                    showContent();
                    
                    Log.d(TAG, "成功加载英文名言: " + result.getContent());
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                
                Log.d(TAG, "使用备用英文名言内容，API错误: " + errorMessage);
                
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isAdded()) return;
                    
                    EnglishQuoteApiService.EnglishQuoteData fallbackData = englishQuoteApiService.getFallbackQuote();
                    titleText.setText("English Quote");
                    contentText.setText(fallbackData.getFormattedContent());
                    showContent();
                    
                    Log.d(TAG, "使用备用英文名言: " + fallbackData.getContent());
                });
            }
        });
    }
    
    /**
     * 测试使用本地数据
     */
    private void testWithLocalData() {
        if (!isAdded()) return;
        
        showLoading(true);
        
        Log.d(TAG, "测试使用本地数据: " + todayContentType.name());
        
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!isAdded()) return;
            
            try {
                switch (todayContentType) {
                    case ESSAY:
                        EssayApiService.EssayData essayData = essayApiService.getFallbackEssay();
                        titleText.setText(essayData.getTitle());
                        contentText.setText(essayData.getContent() + "\n\n——" + essayData.getAuthor());
                        break;
                    case QUOTE:
                        QuoteApiService.QuoteData quoteData = quoteApiService.getFallbackQuote();
                        titleText.setText(quoteData.getTypeTitle());
                        contentText.setText(quoteData.getFormattedContent());
                        break;
                    case POEM:
                        PoemApiService.PoemData poemData = poemApiService.getFallbackPoem();
                        titleText.setText(poemData.getTitle());
                        contentText.setText(poemData.getFormattedContent());
                        break;
                    case ENGLISH_QUOTE:
                        EnglishQuoteApiService.EnglishQuoteData englishData = englishQuoteApiService.getFallbackQuote();
                        titleText.setText("English Quote");
                        contentText.setText(englishData.getFormattedContent());
                        break;
                }
                
                showContent();
                Toast.makeText(getContext(), "使用本地" + getContentTypeName() + "数据", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "测试本地数据时出错", e);
                showError("测试本地数据时出错: " + e.getMessage());
            }
        });
    }
    
    /**
     * 测试错误情况
     */
    private void testWithError() {
        if (!isAdded()) return;
        
        showLoading(true);
        
        Log.d(TAG, "测试错误情况: " + todayContentType.name());
        
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!isAdded()) return;
            
            String errorMessage = "模拟API错误: 测试" + getContentTypeName() + "错误处理";
            showError(errorMessage);
            Toast.makeText(getContext(), "测试错误情况", Toast.LENGTH_SHORT).show();
        });
    }
    
    /**
     * 获取当前内容类型的名称
     */
    private String getContentTypeName() {
        switch (todayContentType) {
            case ESSAY: return "文章";
            case QUOTE: return "名言";
            case POEM: return "诗词";
            case ENGLISH_QUOTE: return "英文名言";
            default: return "内容";
        }
    }
    
    private void showLoading(boolean isLoading) {
        if (!isAdded()) return;
        
        if (isLoading) {
            loadingProgress.setVisibility(View.VISIBLE);
            contentCard.setVisibility(View.GONE);
            errorView.setVisibility(View.GONE);
        } else {
            loadingProgress.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
        }
    }
    
    private void showContent() {
        showLoading(false);
        contentCard.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
        
        // 保存当前内容
        saveCurrentContent();
    }
    
    private void showError(String message) {
        showLoading(false);
        contentCard.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
        errorText.setText(message);
    }
    
    private void shareContent(String title, String content) {
        if (getContext() == null) return;
        
        try {
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, title);
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, title + "\n\n" + content);
            startActivity(android.content.Intent.createChooser(shareIntent, "分享内容"));
        } catch (Exception e) {
            Toast.makeText(getContext(), "分享失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveToNotes(String title, String content) {
        if (getContext() == null) return;
        
        try {
            // 创建Intent跳转到笔记编辑页面
            Intent intent = new Intent(getContext(), com.lianxiangdaimaowang.lumina.note.NoteEditActivity.class);
            
            // 传递标题和内容参数
            intent.putExtra("title", title);
            intent.putExtra("content", content);
            
            // 设置一个默认的主题/科目
            String subject = "";
            switch (todayContentType) {
                case ESSAY:
                    subject = "文章";
                    break;
                case QUOTE:
                    subject = "名言";
                    break;
                case POEM:
                    subject = "诗词";
                    break;
                case ENGLISH_QUOTE:
                    subject = "英文名言";
                    break;
                default:
                    subject = "每日推送";
                    break;
            }
            intent.putExtra("subject", subject);
            
            // 启动笔记编辑活动
            startActivity(intent);
            
            // 显示提示
            Toast.makeText(getContext(), "正在创建新笔记", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "保存到笔记失败", e);
            Toast.makeText(getContext(), "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 如果需要，可以在这里刷新内容
    }
} 