package com.example.campusexpensemanager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusexpensemanager.R;
import com.example.campusexpensemanager.models.ExpenseTemplate;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/**
 * TemplateAdapter for horizontal quick-add expense templates
 * Sprint 5: Shows chips like "🏠 Tiền trọ", "🍜 Ăn sáng"
 */
public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.TemplateViewHolder> {

    private Context context;
    private List<ExpenseTemplate> templates;
    private OnTemplateClickListener listener;

    public interface OnTemplateClickListener {
        void onTemplateClick(ExpenseTemplate template);
    }

    public TemplateAdapter(Context context, List<ExpenseTemplate> templates, OnTemplateClickListener listener) {
        this.context = context;
        this.templates = templates;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TemplateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_expense_template, parent, false);
        return new TemplateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TemplateViewHolder holder, int position) {
        ExpenseTemplate template = templates.get(position);

        holder.tvTemplateName.setText(template.getDisplayText());

        holder.cardTemplate.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTemplateClick(template);
            }

            // Visual feedback
            holder.cardTemplate.setCardElevation(8f);
            holder.cardTemplate.postDelayed(() ->
                    holder.cardTemplate.setCardElevation(2f), 200);
        });
    }

    @Override
    public int getItemCount() {
        return templates.size();
    }

    static class TemplateViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardTemplate;
        TextView tvTemplateName;

        public TemplateViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTemplate = itemView.findViewById(R.id.card_template);
            tvTemplateName = itemView.findViewById(R.id.tv_template_name);
        }
    }
}