package com.bytezap.wobble.alarm;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bytezap.wobble.R;
import com.bytezap.wobble.database.AlarmInstance;
import com.bytezap.wobble.utils.CommonUtils;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@SuppressWarnings("ConstantConditions")
public class MathProblem extends Fragment implements View.OnClickListener {

    private static List<Integer> keys = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 0);
    private final String[] operators = {"+", "-", "x", "/", "%"};
    private final int[][] levelMin = {
            {5, 50, 150, 400},
            {7, 50, 150, 400},
            {2, 11, 30, 130},
            {2, 30, 150, 400},
            {4, 30, 150, 400}};
    private final int[][] levelMax = {
            {40, 150, 400, 900},
            {40, 150, 400, 900},
            {10, 50, 120, 900},
            {30, 150, 400, 900},
            {30, 150, 400, 900}};
    private int level = 0;
    private int answer = 0;
    private int operator = 0;
    private Random random;
    private TextView question;
    private TextView answerTxt, probText;
    private Button[] buttons;
    private ImageButton clearBtn;
    private TextView toastView;
    private ViewPropertyAnimator toastAnimator;
    private AlarmInstance instance;
    private AlarmScreenInterface alarmListener;
    private Context mContext;
    private Vibrator vibrator;
    private Button skip;

    private Handler problemHandler = new Handler();
    private final Runnable mToastRunnable = new Runnable() {
        @Override
        public void run() {
            hideToast();
        }
    };
    private Animation rotateAnim;
    private int probLeft = 1;

    public static MathProblem newInstance() {
        return new MathProblem();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.math_problem, container, false);

        question = rootView.findViewById(R.id.question);
        answerTxt = rootView.findViewById(R.id.edit_math_answer);

        buttons = new Button[]{rootView.findViewById(R.id.btn0),
                rootView.findViewById(R.id.btn1),
                rootView.findViewById(R.id.btn2),
                rootView.findViewById(R.id.btn3),
                rootView.findViewById(R.id.btn4),
                rootView.findViewById(R.id.btn5),
                rootView.findViewById(R.id.btn6),
                rootView.findViewById(R.id.btn7),
                rootView.findViewById(R.id.btn8),
                rootView.findViewById(R.id.btn9),};

        ImageButton enterBtn = rootView.findViewById(R.id.enter);
        ImageButton backBtn = rootView.findViewById(R.id.go_back);
        skip = rootView.findViewById(R.id.problem_skip);
        clearBtn = rootView.findViewById(R.id.clear);
        toastView = rootView.findViewById(R.id.math_wrong_answer);
        probText = rootView.findViewById(R.id.problem_count);
        toastAnimator = toastView.animate();

        enterBtn.setOnClickListener(this);
        backBtn.setOnClickListener(this);
        clearBtn.setOnClickListener(this);
        skip.setOnClickListener(this);

        clearBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!TextUtils.isEmpty(answerTxt.getText())) {
                    answerTxt.setText("");
                    return true;
                }
                return false;
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        random = new Random();
        Collections.shuffle(keys);

        for (int i = 0; i <= 9; i++) {
            buttons[i].setText(String.format(Locale.getDefault(), "%d", keys.get(i)));
            buttons[i].setOnClickListener(this);
        }

        mContext = MathProblem.this.getActivity().getApplicationContext();
        long id = getActivity().getIntent().getLongExtra(AlarmAssistant.ID, -1);
        instance = AlarmAssistant.getInstance(mContext, id);
        vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        rotateAnim = AnimationUtils.loadAnimation(mContext, R.anim.rotate);
        rotateAnim.setDuration(400);

        if (instance != null && alarmListener!=null) {
            if (alarmListener.isDismissFrag()) {
                level = instance.dismissLevel;
                probLeft = instance.mathDismissProb;
                skip.setVisibility(instance.dismissSkip ? View.VISIBLE : View.GONE);
            } else {
                level = instance.snoozeLevel;
                probLeft = instance.mathSnoozeProb;
                skip.setVisibility(instance.snoozeSkip ? View.VISIBLE : View.GONE);
            }
            if (level < 0) {
                level = 0;
            }
            if (probLeft <= 0) {
                probLeft = 1;
            }
            if (probLeft > 25) {
                probLeft = 25;
            }
        }

        if (level == 0) {
            question.setEms(4);
        } else {
            question.setEms(5);
        }

        try {
            question.setText(chooseQuestion());
            probText.setText(getString(R.string.problems_left, probLeft));
        } catch (Exception e) {
            switch (level) {
                case 0:
                    question.setText(String.format(Locale.getDefault(), "%d x %d", 7, 5));
                    answer = 35;
                    break;

                case 1:
                    question.setText(String.format(Locale.getDefault(), "%d - %d", 128, 59));
                    answer = 69;
                    break;

                case 2:
                    question.setText(String.format(Locale.getDefault(), "%d + %d", 272, 359));
                    answer = 631;
                    break;

                case 3:
                    question.setText(String.format(Locale.getDefault(), "%d x %d", 319, 842));
                    answer = 268598;
                    break;
            }
            Log.v("MathProblem", "Could not choose question - " + e.toString());
        }

    }

    private String chooseQuestion() {
        final int ADD_OPERATOR = 0, SUBTRACT_OPERATOR = 1, MULTIPLY_OPERATOR = 2, DIVIDE_OPERATOR = 3, MODULUS_OPERATOR = 4;

        operator = random.nextInt(operators.length);
        int operand1 = getOperand();
        int operand2 = getOperand();
        int count = 0;

        switch (operator) {
            case SUBTRACT_OPERATOR:
                while (count <= 15) {
                    if (operand2 >= operand1 || operand2 == 0) {
                        operand2 = random.nextInt(operand1);
                        count++;
                    } else {
                        break;
                    }
                }
                if (count > 15) {
                    operator = ADD_OPERATOR;
                    operand1 = getOperand();
                    operand2 = getOperand();
                }
                break;

            case DIVIDE_OPERATOR:
                while (count <= 60) {
                    if (operand2 == 0 || (((double) operand1 / (double) operand2) % 1 > 0) || (operand1 == operand2) || operand2 == 1) {
                        operand2 = random.nextInt(operand1) + 5;
                        count++;
                    } else {
                        break;
                    }
                }
                if (count > 60) {
                    operator = MULTIPLY_OPERATOR;
                    operand1 = getOperand();
                    operand2 = getOperand();
                }
                break;

            case MODULUS_OPERATOR:
                while (count <= 15) {
                    if (operand2 == 0  || operand2 >= operand1 || operand2 == 1) {
                        operand2 = random.nextInt(operand1);
                        count++;
                    } else {
                        break;
                    }
                }
                if (count > 15) {
                    operator = ADD_OPERATOR;
                    operand1 = getOperand();
                    operand2 = getOperand();
                }
                break;
        }

        switch (operator) {

            case ADD_OPERATOR:
                answer = operand1 + operand2;
                break;

            case SUBTRACT_OPERATOR:
                answer = operand1 - operand2;
                break;

            case MULTIPLY_OPERATOR:
                answer = operand1 * operand2;
                break;

            case DIVIDE_OPERATOR:
                answer = operand1 / operand2;
                break;

            case MODULUS_OPERATOR:
                answer = operand1 % operand2;
                break;
        }

        return String.format(Locale.getDefault(), "%d %s %d", operand1, operators[operator], operand2);
    }

    private int getOperand() {
        try {
            return random.nextInt(levelMax[operator][level] - levelMin[operator][level] + 1)
                    + levelMin[operator][level];
        } catch (Exception e) {
            Log.v("MathProblem", "Operand not selected: " + e.toString());
            // Return a less random one if choosing the operand fails
            return random.nextInt(25) + 1;
        }
    }

    private void closeFragment() {
        getActivity().getFragmentManager().beginTransaction().remove(MathProblem.this).commit();
        SlidingUpPanelLayout sLayout = getActivity().findViewById(R.id.sliding_layout);
        sLayout.setVisibility(View.VISIBLE);
        sLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        getActivity().findViewById(R.id.alarmScreen_layout).setVisibility(View.VISIBLE);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            alarmListener = (AlarmScreenInterface) getActivity();
        } catch (Exception ignored) {
            Log.e("MathGame", "Listener could be implemented");
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.enter:
                //enter button
                vibrateKey();
                String answerContent = answerTxt.getText().toString();
                if (alarmListener != null && !TextUtils.isEmpty(answerContent)) {
                    //we have an answer
                    final int enteredAnswer = Integer.parseInt(answerContent);
                    if (enteredAnswer == answer) {
                        probLeft--;
                        if (probLeft == 0) {
                            probText.setText(getString(R.string.problems_left, 0));
                            dismiss();
                        } else {
                            answerTxt.setText("");
                            question.setText(chooseQuestion());
                            probText.setText(getString(R.string.problems_left, probLeft));
                        }
                    } else {
                        LinearLayout ansLayout = getActivity().findViewById(R.id.answer_layout);
                        Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.shake);
                        animation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                clearBtn.startAnimation(rotateAnim);
                                showToast();
                                answerTxt.setText("");
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });
                        ansLayout.startAnimation(animation);
                    }
                }
                break;

            case R.id.clear:
                String ans = answerTxt.getText().toString();
                if (!TextUtils.isEmpty(ans)) {
                    answerTxt.setText(removeLastChar(ans));
                }
                break;

            case R.id.go_back:
                vibrateKey();
                closeFragment();
                break;

            case R.id.problem_skip:
                try{
                    question.setText(chooseQuestion());
                    answerTxt.setText("");
                } catch (Exception e){
                    switch (level) {
                        case 0:
                            question.setText(String.format(Locale.getDefault(), "%d x %d", 7, 5));
                            answerTxt.setText("");
                            answer = 35;
                            break;

                        case 1:
                            question.setText(String.format(Locale.getDefault(), "%d - %d", 128, 59));
                            answer = 69;
                            break;

                        case 2:
                            question.setText(String.format(Locale.getDefault(), "%d + %d", 272, 359));
                            answer = 631;
                            break;

                        case 3:
                            question.setText(String.format(Locale.getDefault(), "%d x %d", 319, 842));
                            answer = 268598;
                            break;
                    }
                    Log.v("MathProblem", "Could not choose question - " + e.toString());
                }
                break;

            default:
                //number button
                Button key = (Button) v;
                answerTxt.append(key.getText().toString());
                vibrateKey();
                break;
        }
    }

    private void vibrateKey(){
        if (vibrator!=null && !instance.isVibrate) {
            vibrator.vibrate(40);
        }
    }

    private void dismiss(){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //correct answer is obtained
                CommonUtils.sendAlarmBroadcast(mContext, instance.id, alarmListener.isDismissFrag() ? AlarmService.ALARM_DISMISS : AlarmService.ALARM_SNOOZE);
                getActivity().finish();
            }
        });
    }

    private String removeLastChar(String str) {
        return str.substring(0,str.length()-1);
    }

    private void hideToast() {
        problemHandler.removeCallbacks(mToastRunnable);
        if (toastView.getVisibility() == View.VISIBLE) {
            toastAnimator.cancel();
            toastAnimator
                    .alpha(0)
                    .setDuration(300)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            toastView.setVisibility(View.GONE);
                        }
                    });
        }
    }

    private void showToast() {

        problemHandler.removeCallbacks(mToastRunnable);
        problemHandler.postDelayed(mToastRunnable,
                2000);

        toastView.setVisibility(View.VISIBLE);
        toastView.setAlpha(0);
        toastAnimator.cancel();
        toastAnimator
                .alpha(1)
                .setDuration(1000)
                .setListener(null)
                .start();
    }

    @Override
    public void onDestroy() {
        problemHandler.removeCallbacks(mToastRunnable);
        super.onDestroy();
    }
}

