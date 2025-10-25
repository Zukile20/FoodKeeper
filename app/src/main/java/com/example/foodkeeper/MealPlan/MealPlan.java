package com.example.foodkeeper.MealPlan;

import java.time.LocalDate;

public class MealPlan {


    public long getFridgeID() {
        return fridgeID;
    }

    //composite primary key
    private LocalDate planDay;
    private long fridgeID;

    public MealPlan(LocalDate planDay,long fridgeID) {
        this.planDay = planDay;
        this.fridgeID =fridgeID;
    }

    private Long breakFast=null;//
    private Long lunch=null;//
    private Long dinner=null;//
    private Long snack=null;//



    public LocalDate getPlanDay() {
        return planDay;
    }

    public Long getBreakFast() {
        return breakFast;
    }

    public Long getLunch() {
        return lunch;
    }

    public Long getDinner() {
        return dinner;
    }

    public Long getSnack() {
        return snack;
    }

    public void setSnack(Long snack) {
        this.snack = snack;
    }

    public void setDinner(Long dinner) {
        this.dinner = dinner;
    }

    public void setLunch(Long lunch) {
        this.lunch = lunch;
    }

    public void setBreakFast(Long breakFast) {
        this.breakFast = breakFast;
    }
}
