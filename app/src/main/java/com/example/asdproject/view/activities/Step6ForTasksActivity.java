package com.example.asdproject.view.activities;

import android.os.Bundle;
import android.text.TextUtils;
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

        buildQuestionsUI(inflater);

        btnContinue.setOnClickListener(view -> {
            String finalAnswer = collectAnswers();

            if (getActivity() instanceof Listener) {
                ((Listener) getActivity()).onTaskAnswerEntered(finalAnswer);
            }
        });

        return v;
    }

    /** Creates UI blocks: question + answer box */
    private void buildQuestionsUI(LayoutInflater inflater) {

        if (TextUtils.isEmpty(discussionPrompts)) {
            return;
        }

        // Split prompts by new lines
        String[] lines = discussionPrompts.split("\\n");

        for (String line : lines) {
            if (TextUtils.isEmpty(line.trim())) continue;

            questions.add(line.trim());

            // Question text
            TextView txtQuestion = new TextView(requireContext());
            txtQuestion.setText(line.trim());
            txtQuestion.setTextColor(0xFF085F63);
            txtQuestion.setTextSize(16);
            txtQuestion.setPadding(0, 12, 0, 6);

            // Answer box
            EditText edtAnswer = new EditText(requireContext());
            edtAnswer.setBackgroundResource(R.drawable.step_button_selector);
            edtAnswer.setHint("Write your answer here...");
            edtAnswer.setPadding(16, 16, 16, 16);
            edtAnswer.setMinLines(2);
            edtAnswer.setTextColor(0xFF085F63);

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
}
