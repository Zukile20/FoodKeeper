package com.example.foodkeeper.Meal;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodkeeper.FoodItem.FoodItem;
import com.example.foodkeeper.R;

import java.util.ArrayList;
import java.util.List;

public class FoodSelectionAdapter extends RecyclerView.Adapter<FoodSelectionAdapter.ViewHolder> implements Filterable {

    private Context context;
    private ArrayList<FoodItem> originalList;
    private List<FoodItem> filteredList;
    private Filter searchFilter;
    private OnItemSelectionChangeListener selectionChangeListener;

    // Interface for selection change callbacks
    public interface OnItemSelectionChangeListener {
        void onSelectionChanged(int selectedCount);
        void onItemClick(FoodItem item, int position);
    }

    public FoodSelectionAdapter(Context context, ArrayList<FoodItem> items) {
        this.context = context;
        this.originalList = new ArrayList<>(items);
        this.filteredList = new ArrayList<>(originalList);
    }

    public void setOnItemSelectionChangeListener(OnItemSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.food_item_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodItem item = filteredList.get(position);

        // Set data
        holder.textView.setText(item.getName());

        byte[]  blobUrl = item.getImage();
        Glide.with(context)
                .load(blobUrl)
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.place_holder)
                .into(holder.imgView);
        //holder.imgView.setImageResource(item.getUri());

        // Clear previous listener to prevent unwanted triggers
        holder.checkBox.setOnCheckedChangeListener(null);

        holder.checkBox.setChecked(item.getCheckState());

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Update item state
            item.setChecked(isChecked);

            // Update corresponding item in original list
            updateOriginalListCheckState(item, isChecked);

            // Notify listener about selection change
            if (selectionChangeListener != null) {
                selectionChangeListener.onSelectionChanged(getSelectedItemCount());
            }
        });

        holder.itemView.setOnClickListener(v -> {
            boolean newState = !item.getCheckState();
            item.setChecked(newState);
           holder.checkBox.setChecked(newState);

            // Update original list
            updateOriginalListCheckState(item, newState);

            // Notify listeners
            if (selectionChangeListener != null) {
                selectionChangeListener.onSelectionChanged(getSelectedItemCount());
                selectionChangeListener.onItemClick(item, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    /**
     * ViewHolder class
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView imgView;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.itemName);
            imgView = itemView.findViewById(R.id.imageIcon);
            checkBox = itemView.findViewById(R.id.itemCheckBox);
        }
    }

    /**
     * Update the check state in the original list to maintain consistency
     */
    private void updateOriginalListCheckState(FoodItem item, boolean isChecked) {
        for (FoodItem originalItem : originalList) {
            if (originalItem.getId()==item.getId()) {
                originalItem.setChecked(isChecked);
                break;
            }
        }
    }

    /**
     * Get all selected items
     */

    /**
     * Get selected item IDs
     */
    public ArrayList<String> getSelectedItemIds() {
        ArrayList<String> selectedIds = new ArrayList<>();
        for (FoodItem item : originalList) {
            if (item.getCheckState()) {
                selectedIds.add(String.valueOf(item.getId()));
            }
        }
        return selectedIds;
    }

    /**
     * Get count of selected items
     */
    public int getSelectedItemCount() {
        int count = 0;
        for (FoodItem item : originalList) {
            if (item.getCheckState()) {
                count++;
            }
        }
        return count;
    }
    /**
     * Set selections based on item IDs
     */
    public void setSelectedItems(ArrayList<String> selectedItemIds) {
        if (selectedItemIds == null) return;

        for (FoodItem item : originalList) {
            boolean checked = selectedItemIds.contains(String.valueOf(item.getId()));
            item.setChecked(checked);
        }
        notifyDataSetChanged();

        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(getSelectedItemCount());
        }
    }

    @Override
    public Filter getFilter() {
        if (searchFilter == null) {
            searchFilter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    List<FoodItem> filteredItems = new ArrayList<>();

                    if (constraint == null || constraint.length() == 0) {
                        filteredItems.addAll(originalList);
                    } else {
                        String searchText = constraint.toString().toLowerCase().trim();
                        for (FoodItem item : originalList) {
                            if (item.getName().toLowerCase().contains(searchText)) {
                                filteredItems.add(item);
                            }
                        }
                    }

                    results.values = filteredItems;
                    results.count = filteredItems.size();
                    return results;
                }

                @Override
                @SuppressWarnings("unchecked")
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filteredList.clear();
                    if (results.values != null) {
                        filteredList.addAll((List<FoodItem>) results.values);
                    }
                    notifyDataSetChanged();
                }
            };
        }
        return searchFilter;
    }
}