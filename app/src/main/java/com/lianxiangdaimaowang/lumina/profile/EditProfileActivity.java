package com.lianxiangdaimaowang.lumina.profile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.base.BaseActivity;
import com.lianxiangdaimaowang.lumina.data.LocalDataManager;

/**
 * 编辑个人资料活动
 */
public class EditProfileActivity extends BaseActivity {

    private static final String PREF_STUDENT_IDENTITY = "student_identity";
    private TextInputEditText usernameEdit;
    private RadioGroup identityRadioGroup;
    private LocalDataManager localDataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // 初始化本地数据管理器
        localDataManager = LocalDataManager.getInstance(this);

        // 初始化Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 初始化视图
        usernameEdit = findViewById(R.id.edit_username);
        identityRadioGroup = findViewById(R.id.radio_group_identity);
        MaterialButton saveButton = findViewById(R.id.button_save);

        // 设置已有用户名
        String currentUsername = localDataManager.getCurrentUsername();
        if (currentUsername != null && !currentUsername.equals(getString(R.string.profile_username))) {
            usernameEdit.setText(currentUsername);
        }

        // 加载已有学生身份
        loadStudentIdentity();

        // 设置保存按钮点击事件
        saveButton.setOnClickListener(v -> saveProfile());
    }

    /**
     * 加载已有学生身份
     */
    private void loadStudentIdentity() {
        SharedPreferences prefs = getSharedPreferences("profile_prefs", MODE_PRIVATE);
        int identityValue = prefs.getInt(PREF_STUDENT_IDENTITY, 2); // 默认为初中生
        
        // 根据保存的值选择对应的RadioButton
        switch (identityValue) {
            case 1:
                ((RadioButton) findViewById(R.id.radio_primary)).setChecked(true);
                break;
            case 2:
                ((RadioButton) findViewById(R.id.radio_middle)).setChecked(true);
                break;
            case 3:
                ((RadioButton) findViewById(R.id.radio_high)).setChecked(true);
                break;
            case 4:
                ((RadioButton) findViewById(R.id.radio_college)).setChecked(true);
                break;
            case 5:
                ((RadioButton) findViewById(R.id.radio_master)).setChecked(true);
                break;
            case 6:
                ((RadioButton) findViewById(R.id.radio_phd)).setChecked(true);
                break;
            default:
                ((RadioButton) findViewById(R.id.radio_middle)).setChecked(true);
                break;
        }
    }

    /**
     * 保存个人资料
     */
    private void saveProfile() {
        // 获取用户名
        String username = usernameEdit.getText() != null ? usernameEdit.getText().toString().trim() : "";
        if (username.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_username, Toast.LENGTH_SHORT).show();
            return;
        }

        // 保存用户名
        localDataManager.setUsername(username);

        // 保存学生身份
        int identityValue = 2; // 默认为初中生
        int checkedId = identityRadioGroup.getCheckedRadioButtonId();
        
        if (checkedId == R.id.radio_primary) {
            identityValue = 1; // 小学生
        } else if (checkedId == R.id.radio_middle) {
            identityValue = 2; // 初中生
        } else if (checkedId == R.id.radio_high) {
            identityValue = 3; // 高中生
        } else if (checkedId == R.id.radio_college) {
            identityValue = 4; // 大学生
        } else if (checkedId == R.id.radio_master) {
            identityValue = 5; // 硕士研究生
        } else if (checkedId == R.id.radio_phd) {
            identityValue = 6; // 博士研究生
        }
        
        SharedPreferences.Editor editor = getSharedPreferences("profile_prefs", MODE_PRIVATE).edit();
        editor.putInt(PREF_STUDENT_IDENTITY, identityValue);
        editor.apply();

        // 提示保存成功
        Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}