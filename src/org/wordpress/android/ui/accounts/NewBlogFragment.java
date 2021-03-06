
package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.util.AlertUtil;
import org.wordpress.android.widgets.WPTextView;

public class NewBlogFragment extends NewAccountAbstractPageFragment implements TextWatcher {
    private EditText mSiteUrlTextField;
    private EditText mSiteTitleTextField;
    private WPTextView mSignupButton;
    private WPTextView mProgressTextSignIn;
    private ProgressBar mProgressBarSignIn;

    public NewBlogFragment() {
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (fieldsFilled()) {
            mSignupButton.setEnabled(true);
        } else {
            mSignupButton.setEnabled(false);
        }
    }

    private boolean fieldsFilled() {
        return mSiteUrlTextField.getText().toString().trim().length() > 0
                && mSiteTitleTextField.getText().toString().trim().length() > 0;
    }

    protected void startProgress(String message) {
        mProgressBarSignIn.setVisibility(View.VISIBLE);
        mProgressTextSignIn.setVisibility(View.VISIBLE);
        mSignupButton.setVisibility(View.GONE);
        mProgressBarSignIn.setEnabled(false);
        mProgressTextSignIn.setText(message);
    }

    protected void updateProgress(String message) {
        mProgressTextSignIn.setText(message);
    }

    protected void endProgress() {
        mProgressBarSignIn.setVisibility(View.GONE);
        mProgressTextSignIn.setVisibility(View.GONE);
        mSignupButton.setVisibility(View.VISIBLE);
    }

    private void showSiteUrlError(int messageId) {
        mSiteUrlTextField.setError(getString(messageId));
        mSiteUrlTextField.requestFocus();
    }

    private void showSiteTitleError(int messageId) {
        mSiteTitleTextField.setError(getString(messageId));
        mSiteTitleTextField.requestFocus();
    }

    protected boolean specificShowError(int messageId) {
        switch (getErrorType(messageId)) {
            case TITLE:
                showSiteTitleError(messageId);
                return true;
            case SITE_URL:
                showSiteUrlError(messageId);
                return true;
        }
        return false;
    }

    private boolean checkUserData() {
        // try to create the user
        final String siteTitle = mSiteTitleTextField.getText().toString().trim();
        final String siteUrl = mSiteUrlTextField.getText().toString().trim();

        if (siteTitle.equals("")) {
            mSiteTitleTextField.setError(getString(R.string.required_field));
            mSiteTitleTextField.requestFocus();
            return false;
        }

        if (siteUrl.equals("")) {
            mSiteUrlTextField.setError(getString(R.string.required_field));
            mSiteUrlTextField.requestFocus();
            return false;
        }

        return true;
    }

    OnClickListener signupClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            validateAndCreateUserAndBlog();
        }
    };

    private String titleToUrl(String siteUrl) {
        return siteUrl.replaceAll("[^a-zA-Z0-9]","").toLowerCase();
    }

    private void validateAndCreateUserAndBlog() {
        if (mSystemService.getActiveNetworkInfo() == null) {
            AlertUtil.showAlert(getActivity(), R.string.no_network_title,
                    R.string.no_network_message);
            return;
        }
        if (!checkUserData())
            return;

        startProgress(getString(R.string.validating_site_data));

        final String siteUrl = mSiteUrlTextField.getText().toString().trim();
        final String siteName = mSiteTitleTextField.getText().toString().trim();
        final String language = CreateUserAndBlog.getDeviceLanguage(getActivity().getResources());

        CreateUserAndBlog createUserAndBlog = new CreateUserAndBlog("", "", "",
                siteUrl, siteName, language, restClient, getActivity(), new ErrorListener(),
                new CreateUserAndBlog.Callback() {
                    @Override
                    public void onStepFinished(CreateUserAndBlog.Step step) {
                        if (getActivity() != null) {
                            updateProgress(getString(R.string.create_new_blog_wpcom));
                        }
                    }

                    @Override
                    public void onSuccess(JSONObject createSiteResponse) {
                        if (getActivity() == null) {
                            return ;
                        }
                        endProgress();
                        SetupBlog setupBlog = new SetupBlog();
                        try {
                            JSONObject details = createSiteResponse.getJSONObject("blog_details");
                            String blogName = details.getString("blogname");
                            String xmlRpcUrl = details.getString("xmlrpc");
                            String homeUrl = details.getString("url");
                            String blogId = details.getString("blogid");
                            final SharedPreferences settings = PreferenceManager.
                                    getDefaultSharedPreferences(getActivity());
                            String username = settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, "");
                            String password = WordPressDB.decryptPassword(settings.
                                    getString(WordPress.WPCOM_PASSWORD_PREFERENCE, null));
                            setupBlog.addBlog(blogName, xmlRpcUrl, homeUrl, blogId, username,
                                    password);
                        } catch (JSONException e) {
                            Log.e(WordPress.TAG, "Invalid JSON response from site/new: " + e);
                        }
                        getActivity().setResult(Activity.RESULT_OK);
                        getActivity().finish();
                    }

                    @Override
                    public void onError(int messageId) {
                        if (getActivity() == null) {
                            return ;
                        }
                        endProgress();
                        showError(getString(messageId));
                    }
                });
        createUserAndBlog.startCreateBlogProcess();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout containing a title and body text.
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.create_blog_fragment, container,
                false);

        mSignupButton = (WPTextView) rootView.findViewById(R.id.signup_button);
        mSignupButton.setOnClickListener(signupClickListener);
        mSignupButton.setEnabled(false);

        mProgressTextSignIn = (WPTextView) rootView.findViewById(R.id.nux_sign_in_progress_text);
        mProgressBarSignIn = (ProgressBar) rootView.findViewById(R.id.nux_sign_in_progress_bar);

        mSiteTitleTextField = (EditText) rootView.findViewById(R.id.site_title);
        mSiteUrlTextField = (EditText) rootView.findViewById(R.id.site_url);

        mSiteUrlTextField.addTextChangedListener(this);
        mSiteTitleTextField.addTextChangedListener(this);
        mSiteTitleTextField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // auto fill blog address from title
                mSiteUrlTextField.setText(titleToUrl(mSiteTitleTextField.getText().toString()));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        return rootView;
    }
}