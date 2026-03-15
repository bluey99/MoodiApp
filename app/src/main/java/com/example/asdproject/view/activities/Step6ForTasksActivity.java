package com.example.asdproject.view.activities;

import android.os.Bundle;
import android.text.BidiFormatter;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.example.asdproject.R;

import java.util.ArrayList;
import java.util.List;

public class Step6ForTasksActivity extends Fragment {

    public interface Listener {
        void onTaskAnswerEntered(String answer);
    }

    private static final String ARG_PROMPTS = "ARG_PROMPTS";

    public static Step6ForTasksActivity newInstance(String discussionPrompts) {
        Step6ForTasksActivity fragment = new Step6ForTasksActivity();
        Bundle args = new Bundle();
        args.putString(ARG_PROMPTS, discussionPrompts);
        fragment.setArguments(args);
        return fragment;
    }

    private String discussionPrompts;

    private LinearLayout questionsContainer;
    private AppCompatButton btnContinue;

    // Keep references to answer inputs
    private final List<EditText> answerInputs = new ArrayList<>();
    private final List<String> questions = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            discussionPrompts = args.getString(ARG_PROMPTS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.activity_step6_for_tasks, container, false);

        questionsContainer = v.findViewById(R.id.questionsContainer);
        btnContinue = v.findViewById(R.id.btnContinue);

        buildQuestionsUI();

        btnContinue.setOnClickListener(view -> {
            String finalAnswer = collectAnswers();

            if (getActivity() instanceof Listener) {
                ((Listener) getActivity()).onTaskAnswerEntered(finalAnswer);
            }
        });

        return v;
    }

    /** Creates UI blocks: question + answer box */
    private void buildQuestionsUI() {

        if (TextUtils.isEmpty(discussionPrompts)) {
            return;
        }

        // Split prompts by new lines
        String[] lines = discussionPrompts.split("\\n");

        for (String line : lines) {
            if (TextUtils.isEmpty(line.trim())) continue;

            String questionText = line.trim();
            questions.add(questionText);

            // Question text
            TextView txtQuestion = new TextView(requireContext());
            txtQuestion.setTextColor(0xFF085F63);
            txtQuestion.setTextSize(16);
            txtQuestion.setPadding(0, 12, 0, 6);
            applyContentDirection(txtQuestion, questionText);

            // Answer box
            EditText edtAnswer = new EditText(requireContext());
            edtAnswer.setBackgroundResource(R.drawable.step_button_selector);
            edtAnswer.setHint(getString(R.string.step6_tasks_answer_hint));
            edtAnswer.setPadding(16, 16, 16, 16);
            edtAnswer.setMinLines(2);
            edtAnswer.setTextColor(0xFF085F63);

            // keep answer input friendly to current app language
            edtAnswer.setTextDirection(View.TEXT_DIRECTION_LOCALE);
            edtAnswer.setGravity(Gravity.START);

            questionsContainer.addView(txtQuestion);
            questionsContainer.addView(edtAnswer);

            answerInputs.add(edtAnswer);
        }
    }

    /** Collect answers into ONE formatted string */
    private String collectAnswers() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < questions.size(); i++) {
            String q = questions.get(i);
            String a = answerInputs.get(i).getText().toString().trim();

            sb.append(q).append("\n");
            sb.append(a.isEmpty() ? "-" : a);
            sb.append("\n\n");
        }

        return sb.toString().trim();
    }

    // bayan added here - display task questions according to the language they were written in
    private void applyContentDirection(TextView textView, String text) {
        if (text == null) {
            textView.setText("");
            return;
        }

        boolean isRtl = isRtlText(text);

        BidiFormatter bidi = BidiFormatter.getInstance(isRtl);
        textView.setText(bidi.unicodeWrap(text));

        if (isRtl) {
            textView.setTextDirection(View.TEXT_DIRECTION_RTL);
            textView.setGravity(Gravity.RIGHT);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        } else {
            textView.setTextDirection(View.TEXT_DIRECTION_LTR);
            textView.setGravity(Gravity.LEFT);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        }
    }

    // bayan added here - detect direction from the first strong character
    private boolean isRtlText(String text) {
        if (text == null) return false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            byte dir = Character.getDirectionality(c);

            if (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                    dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                return true;
            }

            if (dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT) {
                return false;
            }
        }

        return false;
    }
}