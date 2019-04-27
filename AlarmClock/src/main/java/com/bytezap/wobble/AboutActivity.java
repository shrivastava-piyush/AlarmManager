package com.bytezap.wobble;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.support.v7.widget.Toolbar;
import android.transition.AutoTransition;
import android.transition.Slide;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bytezap.wobble.utils.BitmapController;
import com.bytezap.wobble.utils.CommonUtils;
import com.bytezap.wobble.utils.DialogSupervisor;
import com.bytezap.wobble.utils.ToastGaffer;

public class AboutActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageButton rateApp;
    private ImageButton shareApp;
    private ImageButton mailApp;
    private int count = 3;
    private BitmapDrawable background;

    private String ver;
    private final Handler animHandler = new Handler();
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.popup_enter);
            switch (count) {
                case 3:
                    rateApp.startAnimation(animation);
                    rateApp.setVisibility(View.VISIBLE);
                    break;

                case 2:
                    shareApp.startAnimation(animation);
                    shareApp.setVisibility(View.VISIBLE);
                    break;

                case 1:
                    mailApp.startAnimation(animation);
                    mailApp.setVisibility(View.VISIBLE);
                    break;

                default:
                    break;
            }
            count--;
            animHandler.postDelayed(runnable, 300);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Resources res = getResources();
        if (BitmapController.isAnimation()) {
            if (CommonUtils.isLOrLater()) {
                getWindow().setEnterTransition(CommonUtils.isPortrait(res) ? new AutoTransition() : new Slide());
            } else {
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        }

        CommonUtils.setLanguage(res, CommonUtils.getLangCode());
        setContentView(R.layout.about_app_layout);

        background = new BitmapDrawable(res, BitmapFactory.decodeResource(res, R.drawable.about_bg));
        try {
            getWindow().setBackgroundDrawable(background);
        } catch (NullPointerException e){
            getWindow().getDecorView().setBackgroundColor(Color.parseColor(Clock.ACTIVITY_COLOR));
        }

        Toolbar toolbar = findViewById(R.id.toolbar_about);
        setSupportActionBar(toolbar);

        rateApp = findViewById(R.id.about_app_rate);
        shareApp = findViewById(R.id.about_app_share);
        mailApp = findViewById(R.id.about_app_mail);
        TextView version = findViewById(R.id.app_version);
        TextView credits = findViewById(R.id.about_app_credits);
        TextView license = findViewById(R.id.about_app_licenses);
        TextView policy = findViewById(R.id.privacy_policy);
        TextView localization = findViewById(R.id.about_app_localization);

        ver = getString(R.string.app_name) + " " + getString(R.string.app_version, 1.0);
        version.setText(getString(R.string.app_version, 1.0));
        credits.setPaintFlags(credits.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        license.setPaintFlags(credits.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        policy.setPaintFlags(credits.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        localization.setPaintFlags(credits.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        if (CommonUtils.isLOrLater()) {
            try {
                TypedValue outValue = new TypedValue();
                getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
                shareApp.setBackgroundResource(outValue.resourceId);
                rateApp.setBackgroundResource(outValue.resourceId);
                mailApp.setBackgroundResource(outValue.resourceId);
            }  catch (Throwable b) {
                b.printStackTrace();
            }
        }

        if (BitmapController.isAnimation()) {
            animHandler.postDelayed(runnable, 600);
        } else {
            shareApp.setVisibility(View.VISIBLE);
            rateApp.setVisibility(View.VISIBLE);
            mailApp.setVisibility(View.VISIBLE);
        }

        rateApp.setOnClickListener(this);
        shareApp.setOnClickListener(this);
        mailApp.setOnClickListener(this);
        credits.setOnClickListener(this);
        license.setOnClickListener(this);
        policy.setOnClickListener(this);
        localization.setOnClickListener(this);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        if (BitmapController.isAnimation()) {
            overridePendingTransition(0, R.anim.fade_out);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.about_app_rate:
                try {
                    Intent rateintent = new Intent("android.intent.action.VIEW",
                            Uri.parse(Clock.APP_URL));
                    startActivity(rateintent);
                } catch (Exception ignored){}
                break;

            case R.id.about_app_share:
                try {
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra("android.intent.extra.SUBJECT", R.string.app_name);
                    share.putExtra("android.intent.extra.TEXT",
                            String.valueOf("\n" + getString(R.string.app_share_message) + " " + Clock.APP_URL + "\n\n"));
                                    startActivity(share);
                } catch (Exception ignored) {}
                break;

            case R.id.about_app_mail:
                try {
                    Intent mail = new Intent(Intent.ACTION_VIEW);
                    mail.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
                    mail.setDataAndType(Uri.parse("Contact me"), "text/plain");
                    mail.putExtra(Intent.EXTRA_EMAIL, new String[]{"wobbletheclock@gmail.com"});
                    mail.putExtra(Intent.EXTRA_SUBJECT, ver);
                    startActivity(mail);
                } catch (Exception e) {
                    ToastGaffer.showToast(getApplicationContext(), getString(R.string.gmail_error));
                }
                break;

            case R.id.about_app_credits:
                ContextThemeWrapper wrapper = new ContextThemeWrapper(AboutActivity.this, R.style.AlertDialogStyle);
                final LayoutInflater inflater = (LayoutInflater) wrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View credits = inflater.inflate(R.layout.credits_dialog, null);
                AlertDialog dialog = new AlertDialog.Builder(wrapper).create();
                dialog.setView(credits);
                dialog.setTitle(R.string.app_credits);
                dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.default_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                DialogSupervisor.setDialog(dialog);
                dialog.show();
                break;

            case R.id.about_app_licenses:
                ContextThemeWrapper licenseWrapper = new ContextThemeWrapper(AboutActivity.this, R.style.AlertDialogStyle);
                final LayoutInflater layoutInflater = (LayoutInflater) licenseWrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                AlertDialog licenseDialog = new AlertDialog.Builder(licenseWrapper).create();
                View license = layoutInflater.inflate(R.layout.licenses_dialog, null);
                licenseDialog.setView(license);
                licenseDialog.setTitle(R.string.app_licenses);
                licenseDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.default_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                DialogSupervisor.setDialog(licenseDialog);
                licenseDialog.show();
                break;

            case R.id.privacy_policy:
                ContextThemeWrapper policyWrapper = new ContextThemeWrapper(AboutActivity.this, R.style.AlertDialogStyle);
                final LayoutInflater inflator = (LayoutInflater) policyWrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                AlertDialog policyDialog = new AlertDialog.Builder(policyWrapper).create();
                View policy = inflator.inflate(R.layout.policy_dialog, null);
                policyDialog.setView(policy);
                policyDialog.setTitle(R.string.privacy_policy);
                policyDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.default_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                DialogSupervisor.setDialog(policyDialog);
                policyDialog.show();
                break;

            case R.id.about_app_localization:
                try {
                    Intent mail = new Intent(Intent.ACTION_VIEW);
                    mail.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
                    mail.setDataAndType(Uri.parse("Localization"), "text/plain");
                    mail.putExtra(Intent.EXTRA_EMAIL, new String[]{"wobbletheclock@gmail.com"});
                    mail.putExtra(Intent.EXTRA_SUBJECT, ver);
                    startActivity(mail);
                } catch (Exception e) {
                    ToastGaffer.showToast(getApplicationContext(), getString(R.string.gmail_error));
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        animHandler.removeCallbacks(runnable);
    }

    @Override
    protected void onDestroy() {
        if (background!=null) {
            background.getBitmap().recycle();
            background = null;
        }
        DialogSupervisor.cancelDialog();
        super.onDestroy();
    }
}
