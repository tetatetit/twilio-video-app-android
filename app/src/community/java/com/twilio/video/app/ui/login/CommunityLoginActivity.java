/*
 * Copyright (C) 2019 Twilio, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twilio.video.app.ui.login;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import androidx.core.content.res.ResourcesCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import com.google.android.material.textfield.TextInputEditText;
import com.twilio.video.app.R;
import com.twilio.video.app.auth.Authenticator;
import com.twilio.video.app.auth.LoginEvent.CommunityLoginEvent;
import com.twilio.video.app.auth.LoginResult.CommunityLoginSuccessResult;
import com.twilio.video.app.base.BaseActivity;
import com.twilio.video.app.ui.room.RoomActivity;
import com.twilio.video.app.util.InputUtils;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import javax.inject.Inject;
import timber.log.Timber;

// TODO Create view model and fragment for this screen
public class CommunityLoginActivity extends BaseActivity {

    @Inject Authenticator authenticator;

    @BindView(R.id.community_login_screen_progressbar)
    ProgressBar progressBar;

    @BindView(R.id.community_login_screen_name_edittext)
    TextInputEditText nameEditText;

    @BindView(R.id.community_login_screen_passcode_edittext)
    TextInputEditText passcodeEditText;

    @BindView(R.id.community_login_screen_login_button)
    Button loginButton;

    CompositeDisposable disposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.development_activity_login);
        ButterKnife.bind(this);
        if (authenticator.loggedIn()) startLobbyActivity();
    }

    @OnTextChanged(R.id.community_login_screen_name_edittext)
    public void onTextChanged(Editable editable) {
        enableLoginButton(!nameEditText.getText().toString().isEmpty());
    }

    @OnClick(R.id.community_login_screen_login_button)
    public void onLoginButton(View view) {
        String identity = nameEditText.getText().toString();
        String passcode = passcodeEditText.getText().toString();
        if (areIdentityAndPasscodeValid(identity, passcode)) {
            login(identity, passcode);
        }
    }

    private boolean areIdentityAndPasscodeValid(String identity, String passcode) {
        return !identity.isEmpty() && !passcode.isEmpty();
    }

    private void login(String identity, String passcode) {
        preLoginViewState();

        disposable.add(
                authenticator
                        .login(new CommunityLoginEvent(identity, passcode))
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally(this::postLoginViewState)
                        .subscribe(
                                loginResult -> {
                                    if (loginResult instanceof CommunityLoginSuccessResult)
                                        startLobbyActivity();
                                },
                                exception -> {
                                    displayAuthError();
                                    Timber.e(exception);
                                }));
    }

    private void preLoginViewState() {
        InputUtils.hideKeyboard(this);
        enableLoginButton(false);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void postLoginViewState() {
        progressBar.setVisibility(View.GONE);
        enableLoginButton(true);
    }

    private void enableLoginButton(boolean isEnabled) {
        if (isEnabled) {
            loginButton.setTextColor(Color.WHITE);
            loginButton.setEnabled(true);
        } else {
            loginButton.setTextColor(
                    ResourcesCompat.getColor(getResources(), R.color.colorButtonText, null));
            loginButton.setEnabled(false);
        }
    }

    private void startLobbyActivity() {
        RoomActivity.startActivity(this, getIntent().getData());
        finish();
    }

    private void displayAuthError() {
        new AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                .setTitle(getString(R.string.login_screen_auth_error_title))
                .setMessage(getString(R.string.login_screen_auth_error_desc))
                .setPositiveButton("OK", null)
                .show();
    }
}
