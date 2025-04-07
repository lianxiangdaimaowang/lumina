package com.lianxiangdaimaowang.lumina.review;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.lianxiangdaimaowang.lumina.R;
import com.lianxiangdaimaowang.lumina.model.ReviewPlan;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewPlanAdapter extends RecyclerView.Adapter<ReviewPlanAdapter.ReviewPlanViewHolder> {
    private final List<ReviewPlan> reviewPlans;
    private final OnReviewPlanClickListener listener;
    private final SimpleDateFormat dateFormat;

    public ReviewPlanAdapter(List<ReviewPlan> reviewPlans, OnReviewPlanClickListener listener) {
        this.reviewPlans = reviewPlans;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    @NonNull
    @Override
    public ReviewPlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review_plan, parent, false);
        return new ReviewPlanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewPlanViewHolder holder, int position) {
        ReviewPlan reviewPlan = reviewPlans.get(position);
        holder.bind(reviewPlan);
    }

    @Override
    public int getItemCount() {
        return reviewPlans.size();
    }

    class ReviewPlanViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final TextView dateText;
        private final TextView descriptionText;
        private final Chip subjectChip;
        private final MaterialButton reviewButton;
        private final MaterialButton completeButton;
        private final MaterialButton deleteButton;

        ReviewPlanViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.title_text);
            dateText = itemView.findViewById(R.id.date_text);
            descriptionText = itemView.findViewById(R.id.description_text);
            subjectChip = itemView.findViewById(R.id.subject_chip);
            reviewButton = itemView.findViewById(R.id.btn_review);
            completeButton = itemView.findViewById(R.id.btn_complete);
            deleteButton = itemView.findViewById(R.id.btn_delete);

            reviewButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onReviewClick(reviewPlans.get(position));
                }
            });

            completeButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onCompleteClick(reviewPlans.get(position));
                }
            });
            
            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(reviewPlans.get(position));
                }
            });
        }

        void bind(ReviewPlan reviewPlan) {
            titleText.setText(reviewPlan.getNoteTitle());
            descriptionText.setText(reviewPlan.getNoteContent());
            
            // 替代原有的subject
            subjectChip.setVisibility(View.GONE);

            // 显示下一次复习日期
            Date nextReviewDate = reviewPlan.getNextReviewDate();
            if (nextReviewDate != null) {
                dateText.setText(dateFormat.format(nextReviewDate));
            } else {
                dateText.setText(R.string.completed);
            }
            
            // 如果复习计划已完成，隐藏完成按钮
            if (reviewPlan.isCompleted()) {
                completeButton.setVisibility(View.GONE);
            } else {
                completeButton.setVisibility(View.VISIBLE);
            }
        }
    }

    public interface OnReviewPlanClickListener {
        void onReviewClick(ReviewPlan reviewPlan);
        void onCompleteClick(ReviewPlan reviewPlan);
        void onDeleteClick(ReviewPlan reviewPlan);
    }
} 