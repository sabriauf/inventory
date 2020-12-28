package lk.sabri.inventory.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import lk.sabri.inventory.R;
import lk.sabri.inventory.data.InventoryDatabase;
import lk.sabri.inventory.data.LoginData;
import lk.sabri.inventory.util.Constants;

public class LoginActivity extends AppCompatActivity {

    private EditText edtUserName;
    private EditText edtPassword;
    private ProgressBar progress;

    private boolean isLoginProcess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initView();
        loadData();
    }

    private void initView() {
        edtUserName = findViewById(R.id.edt_username);
        edtPassword = findViewById(R.id.edt_password);
        progress = findViewById(R.id.prg_loading);

        findViewById(R.id.btn_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doLogin();
            }
        });

        edtPassword.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    doLogin();
                    return true;
                }
                return false;
            }
        });
    }

    private void doLogin() {
        if(isLoginProcess)
            return;

        isLoginProcess = true;

        if(edtUserName.getText().toString().equals("evergreen") && edtPassword.getText().toString().equals("evergreen_2020")) {
            proceedLogin();
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    InventoryDatabase database = InventoryDatabase.getInstance(LoginActivity.this);
                    LoginData data = database.loginDAO().loadAllByUsername(edtUserName.getText().toString());
                    if(data != null && edtPassword.getText().toString().equals(data.getPassword()))
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                proceedLogin();
                            }
                        });
                    else {
                        isLoginProcess = false;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(LoginActivity.this, "Username or password is incorrect", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }).start();
        }
    }

    private void loadData() {
        SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        String lastLoggedUser = preferences.getString(Constants.PREFERENCE_LAST_USER, "");
        edtUserName.setText(lastLoggedUser);
    }

    private void saveUser(String userName) {
        SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        preferences.edit().putString(Constants.PREFERENCE_LAST_USER, userName).apply();
    }

    private void proceedLogin() {
        saveUser(edtUserName.getText().toString());
        progress.setVisibility(View.VISIBLE);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }
        }, 1000);
    }
}