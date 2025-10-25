package com.example.foodkeeper.MealPlan;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.example.foodkeeper.MealPlan.CalendarUtils.daysInWeekArray;
import static com.example.foodkeeper.MealPlan.CalendarUtils.monthYearFromDate;
import static com.example.foodkeeper.MealPlan.CalendarUtils.selectedDate;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.foodkeeper.FoodkeeperUtils.Database;
import com.example.foodkeeper.FoodkeeperUtils.DeleteConfirmationDialog;
import com.example.foodkeeper.Meal.Meal;
import com.example.foodkeeper.Meal.UpdateMealActivity;
import com.example.foodkeeper.Meal.ViewMealActivity;
import com.example.foodkeeper.R;
import com.example.foodkeeper.Register.SessionManager;
import com.example.foodkeeper.ViewMeals.mealsViewActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;

public class WeeklyViewActivity extends AppCompatActivity implements CalendarAdapter.OnItemListener
{
    private TextView monthYearText;
    private RecyclerView calendarRecyclerView;
    private LinearLayout populatedState;

    private TextView selectedDateText;
    private Button backBtn,breakFastBtn, lunchBtn,dinnerBtn,snackBtn,deleteBtn;
    private FloatingActionButton addButton;

    Context context ;
    private Database db;
    private SessionManager sess;
    ActivityResultLauncher<Intent> launcher;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly_view);
        if (CalendarUtils.selectedDate == null) {
            CalendarUtils.selectedDate = LocalDate.now();
        }

        initWidgets();
        initLayouts();
        setUpListeners();
        setupMonthlyViewLauncher();
        try {
            setWeekView();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initWidgets() {
        db = Database.getInstance(this);
        sess= new SessionManager(this);

        backBtn = findViewById(R.id.back_button);
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);
        monthYearText = findViewById(R.id.monthYearTV);
        selectedDateText = findViewById(R.id.selectedDayText);
        populatedState = findViewById(R.id.populatedStateView);

        breakFastBtn = findViewById(R.id.breakFastBtn);
        lunchBtn = findViewById(R.id.lunchBtn);
        dinnerBtn = findViewById(R.id.DinnerBtn);
        snackBtn = findViewById(R.id.snackBtn);

        addButton =findViewById(R.id.addButton);
        deleteBtn = findViewById(R.id.deleteBtn);


    }
    private void initLayouts()
    {
        breakFastLayout =findViewById(R.id.breakfastContainer);
        lunchLayout = findViewById(R.id.lunchContainer);
        dinnerLayout = findViewById(R.id.dinnerContainer);
        snackLayout =findViewById(R.id.snackContainer);

        breakFastDisplay = findViewById(R.id.breakFastDisplay);
        lunchDisplay = findViewById(R.id.lunchDisplay);
        dinnerDisplay = findViewById(R.id.dinnerDisplay);
        snackDisplay = findViewById(R.id.snackDisplay);

    }

    private void setupResultslauncher() {
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result ->
                {
                    RestoreLayouts();
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        long mealID = data.getLongExtra("selectedMeal",0);
                        long fridgeId = db.getConnectedFridgeForUser(sess.getUserEmail()).getId();
                        Meal meal =db.getMealWithFoodItems(mealID);
                        String type = data.getStringExtra("mealType");
                        db.addMealToPlan(meal.getMealID(), CalendarUtils.selectedDate, type,fridgeId);

                        meal.setLastUsed(selectedDate);
                        db.updateMeal(meal);

                        MealPlan mealPlan = db.getMealPlanForDay(CalendarUtils.selectedDate,fridgeId);
                        if (mealPlan != null) {
                            if((mealPlan.getBreakFast()==null && mealPlan.getLunch()==null && mealPlan.getDinner()==null && mealPlan.getSnack()==null ))// meal plan with no meals must not be stored in the database1
                            {
                                db.deleteMealplan(selectedDate,fridgeId);//check if the meal does contain at least one meal ,otherwise delete it
                            }
                            if (mealPlan.getBreakFast() != null) {
                                //Obtain the breakfast meal from the database
                                Meal breakfastMeal = db.getMealWithFoodItems(mealPlan.getBreakFast());
                                try {
                                    showMealLayout(breakFastDisplay, breakfastMeal, breakFastLayout);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            if (mealPlan.getLunch() != null) {
                                Meal lunchMeal = db.getMealWithFoodItems(mealPlan.getLunch());
                                try {
                                    showMealLayout(lunchDisplay, lunchMeal, lunchLayout);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }

                            }
                            if (mealPlan.getDinner() != null) {
                                Meal dinnnerMeal = db.getMealWithFoodItems(mealPlan.getDinner());
                                try {
                                    showMealLayout(dinnerDisplay, dinnnerMeal, lunchLayout);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            if (mealPlan.getSnack() != null) {
                                Meal snackMeal = db.getMealWithFoodItems(mealPlan.getSnack());
                                try {
                                    showMealLayout(snackDisplay, snackMeal, snackLayout);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }

                }
        );
    }

    private void RestoreLayouts() {

        breakFastDisplay.setVisibility(GONE);
        lunchDisplay.setVisibility(GONE);
        dinnerDisplay.setVisibility(GONE);
        snackDisplay.setVisibility(GONE);
       // deleteBtn.setVisibility(GONE);

        breakFastLayout.setVisibility(VISIBLE);
        lunchLayout.setVisibility(VISIBLE);
        dinnerLayout.setVisibility(VISIBLE);
        snackLayout.setVisibility(VISIBLE);
        addButton.setVisibility(VISIBLE);
    }
    private ActivityResultLauncher<Intent> monthlyViewLauncher;

    private void setupMonthlyViewLauncher() {
        monthlyViewLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        try {
                            setWeekView();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
        );
    }

    private void showMonthly()
    {
        Intent intent = new Intent(this, MonthlyViewActivity.class);
        monthlyViewLauncher.launch(intent);
        overridePendingTransition(R.anim.fade_in, 0);
    }
    FrameLayout breakFastLayout ;
    FrameLayout lunchLayout ;
    FrameLayout dinnerLayout ;
    FrameLayout snackLayout ;

    FrameLayout breakFastDisplay ;
    FrameLayout lunchDisplay ;
    FrameLayout dinnerDisplay ;
    FrameLayout snackDisplay ;


    private void setUpListeners()
    {
        context= getApplicationContext();
        setupResultslauncher();
        backBtn.setOnClickListener(v->finish());
        monthYearText.setOnClickListener(view-> showMonthly());
        breakFastBtn.setOnClickListener(View -> {
           Intent requestIntent =new Intent(this, mealsViewActivity.class);
           requestIntent.putExtra("mealType","Breakfast");
            requestIntent.putExtra("ACTIVITY_MODE",1);

            launcher.launch(requestIntent);
        });
        lunchBtn.setOnClickListener(View -> {
            Intent requestIntent =new Intent(this,mealsViewActivity.class);
            requestIntent.putExtra("mealType","Lunch");
            requestIntent.putExtra("ACTIVITY_MODE",1);
            launcher.launch(requestIntent);
        });
        dinnerBtn.setOnClickListener(View -> {
            Intent requestIntent =new Intent(this,mealsViewActivity.class);
            requestIntent.putExtra("mealType","Dinner");
            requestIntent.putExtra("ACTIVITY_MODE",1);

            launcher.launch(requestIntent);
        });;
        snackBtn.setOnClickListener(View -> {
            Intent requestIntent =new Intent(this,mealsViewActivity.class);
            requestIntent.putExtra("mealType","Snack");
            requestIntent.putExtra("ACTIVITY_MODE",1);
            launcher.launch(requestIntent);
        });
        deleteBtn.setOnClickListener(View->
        {
            //delete the meal plan for the selected date
            DeleteConfirmationDialog dialog =DeleteConfirmationDialog.newInstance("Meal plan","","");


            dialog.setOnDeleteConfirmListener(new DeleteConfirmationDialog.OnDeleteConfirmListener() {
                @Override
                public void onDeleteConfirmed() {
                    db.deleteMealplan(selectedDate,db.getConnectedFridgeForUser(sess.getUserEmail()).getId());
                    if (context instanceof AppCompatActivity) {

                        Toast.makeText(context,"Meal plan deleted successfully", Toast.LENGTH_SHORT).show();
                    }
                    populatedState.setVisibility(INVISIBLE);
                    addButton.setVisibility(VISIBLE);
                }
                @Override
                public void onDeleteCancelled()
                {
                    //Do nothing
                    if (context instanceof AppCompatActivity) {

                        Toast.makeText(context,"Delete cancelled", Toast.LENGTH_SHORT).show();
                    }
                }

            });

            context = View.getContext();
            if (context instanceof AppCompatActivity) {
                dialog.show(((AppCompatActivity) context).getSupportFragmentManager(), "delete_dialog");
            }

        });

    }
    private void setWeekView() throws Exception {

        monthYearText.setText(monthYearFromDate(CalendarUtils.selectedDate));
        ArrayList<LocalDate> days = daysInWeekArray(CalendarUtils.selectedDate);

        CalendarAdapter calendarAdapter = new CalendarAdapter(days, this);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), 7);
        calendarRecyclerView.setLayoutManager(layoutManager);
        calendarRecyclerView.setAdapter(calendarAdapter);

        setupEventsForDay();

        updateSelectedDayText();//initializes selected day
    }
    private void updateSelectedDayText()
    {
        String formatted = CalendarUtils.formattedDayText(selectedDate);
        selectedDateText.setText(formatted);
    }

    public void prevWeekAction(View view) throws Exception {
        CalendarUtils.selectedDate = CalendarUtils.selectedDate.minusWeeks(1);
        setWeekView();
    }

    public void nextWeekAction(View view) throws Exception {
        CalendarUtils.selectedDate = CalendarUtils.selectedDate.plusWeeks(1);
        setWeekView();
    }
    private void showMealLayout(ViewGroup container, Meal meal, ViewGroup placeholder) throws IOException {
        if(meal != null) {
            placeholder.setVisibility(GONE);
            container.setVisibility(VISIBLE);
            container.setMinimumHeight(50);
            View mealView = LayoutInflater.from(this).inflate(R.layout.meal_event_cell, container, false);
            TextView mealName = mealView.findViewById(R.id.eventCellTV);
            ImageView mealImage = mealView.findViewById(R.id.mealImageView);
            ImageView optButton = mealView.findViewById(R.id.editMealBtn);

            mealName.setText(meal.getMealName());
            if (meal.getUri() != null) {//since a meal might not have an image

             loadMealImage(mealImage,meal);

            } else {
                mealImage.setImageResource(R.drawable.place_holder);
            }
            mealView.setOnClickListener(v->
            {
                Intent intent = new Intent(this, ViewMealActivity.class);
                intent.putExtra("viewMeal",meal.getMealID());
                launcher.launch(intent);

            });
            container.addView(mealView);
            optButton.setOnClickListener(v->
            {
                PopupMenu popup = new PopupMenu(this, optButton); // anchorView is the view where popup will appear
                popup.getMenuInflater().inflate(R.menu.meal_menu, popup.getMenu());
                try {
                    Field mFieldPopup = popup.getClass().getDeclaredField("mPopup");
                    mFieldPopup.setAccessible(true);
                    Object mPopup = mFieldPopup.get(popup);
                    mPopup.getClass()
                            .getDeclaredMethod("setForceShowIcon", boolean.class)
                            .invoke(mPopup, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    MealPlan plan = db.getMealPlanForDay(selectedDate,db.getConnectedFridgeForUser(sess.getUserEmail()).getId());
                    if (id == R.id.delete) {
                        int containerId = container.getId();
                       if(containerId==R.id.breakFastDisplay)
                       {
                        plan.setBreakFast(null);
                       }
                       else if(containerId==R.id.lunchDisplay)
                       {
                           plan.setLunch(null);
                       }
                       else if(containerId==R.id.dinnerDisplay)
                       {
                           plan.setDinner(null);
                       }
                       else
                       {
                           plan.setSnack(null);
                       }
                        if((plan.getBreakFast()==null && plan.getLunch()==null && plan.getDinner()==null && plan.getSnack()==null ))// meal plan with no meals must not be stored in the database1
                        {
                            db.deleteMealplan(selectedDate,db.getConnectedFridgeForUser(sess.getUserEmail()).getId());
                        }
                        else {
                            db.updateMealPlan(plan);
                        }
                       RestoreLayouts();
                        try {
                            setupEventsForDay();//updates the ui
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        Toast.makeText(this, "Meal removed", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    else if (id == R.id.edit) {
                        Intent intent = new Intent(this, UpdateMealActivity.class);
                        intent.putExtra("EditMeal",meal.getMealID());
                        startActivity(intent);
                        Toast.makeText(this, "Edit", Toast.LENGTH_SHORT).show();
                        return true;

                    }

                    return false;
                });
                popup.show();

            });
        }

    }



    @Override
    protected void onResume()
    {
        super.onResume();
        try {
            db.deleteExpiredMealPlans();
            setupEventsForDay();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private void setupEventsForDay()throws Exception
    {
        //gets the meal plan for a selected day
        RestoreLayouts();
        long fridgeID = db.getConnectedFridgeForUser(sess.getUserEmail()).getId();
        MealPlan mealPlan = db.getMealPlanForDay(CalendarUtils.selectedDate,fridgeID);
        if(mealPlan!=null)
        {
            addButton.setVisibility(GONE);//The add button is set to be invisible since a meal plan already exist for this day
            populatedState.setVisibility(VISIBLE);

            if((mealPlan.getBreakFast()==null && mealPlan.getLunch()==null && mealPlan.getDinner()==null && mealPlan.getSnack()==null ))// meal plan with no meals must not be stored in the database1
            {
                db.deleteMealplan(selectedDate,fridgeID);//check if the meal does contain at least one meal ,otherwise delete it
                return;
            }
            if(mealPlan.getBreakFast()!=null)
            {
                //Obtain the breakfast meal from the database
               Meal breakfastMeal =db.getMealWithFoodItems(mealPlan.getBreakFast());
               showMealLayout(breakFastDisplay,breakfastMeal,breakFastLayout);
            }
            if(mealPlan.getLunch()!=null )
            {
                Meal lunchMeal =db.getMealWithFoodItems(mealPlan.getLunch());
                showMealLayout(lunchDisplay,lunchMeal,lunchLayout);

            }
            if(mealPlan.getDinner()!=null)
            {
                Meal dinnnerMeal =db.getMealWithFoodItems(mealPlan.getDinner());
                showMealLayout(dinnerDisplay,dinnnerMeal,dinnerLayout);
            }
            if(mealPlan.getSnack()!=null)
            {
                Meal snackMeal =db.getMealWithFoodItems(mealPlan.getSnack());
                showMealLayout(snackDisplay,snackMeal,snackLayout);
            }

        }
        else

        {

            if(selectedDate.isBefore(LocalDate.now())) {
                addButton.setVisibility(View.INVISIBLE);
            } else {
                addButton.setVisibility(View.VISIBLE);
            }
            populatedState.setVisibility(GONE);
        }
    }
    private void loadMealImage(ImageView imageView, Meal meal) {
        String imagePath = meal.getUri();


        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(imagePath);

            if (!imageFile.exists()) {
                imageView.setImageResource(R.drawable.place_holder);
                return;
            }


            Glide.with(this)
                    .load(imageFile)
                    .placeholder(R.drawable.place_holder)
                    .error(R.drawable.place_holder)
                    .fallback(R.drawable.place_holder)
                    .override(64, 64)
                    .centerCrop()
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            if (e != null) {
                                Log.e("ImageLoad", "Glide error details: " + e.getMessage());
                                e.printStackTrace();
                            }
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d("ImageLoad", "Image loaded successfully from: " + imagePath);
                            return false;
                        }
                    })
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.place_holder);
        }
    }

    public void newMealPlanAction(View view)
    {
       Intent intent = new Intent(this, MealPlanActivity.class);
       launcher.launch(intent);

    }

    public void onItemClick(int position, LocalDate date)
    {
        CalendarUtils.selectedDate = date;
        updateSelectedDayText();
        try {
            setWeekView();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}