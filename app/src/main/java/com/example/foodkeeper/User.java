package com.example.foodkeeper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;

public class User implements Serializable {

    private String name;
    private String surname;
    private String email;
    private String phone;
    private String password;
    private byte[] profileImage;

    public User() {
    }

    public User(String name, String surname, String email, String phone, String password) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.profileImage = null;
    }

    public User(String name, String surname, String email, String phone, String password, byte[] profileImage) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.profileImage = profileImage;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPassword() {
        return password;
    }

    public byte[] getProfileImage() {
        return profileImage;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setProfileImage(byte[] profileImage) {
        this.profileImage = profileImage;
    }

    public String getFullName() {
        if (name == null && surname == null) {
            return "";
        } else if (name == null) {
            return surname;
        } else if (surname == null) {
            return name;
        } else {
            return name + " " + surname;
        }
    }



    public boolean hasProfileImage() {
        return profileImage != null && profileImage.length > 0;
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", hasProfileImage=" + hasProfileImage() +
                '}';
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return email != null ? email.equals(user.email) : user.email == null;
    }
    @Override
    public int hashCode() {
        return email != null ? email.hashCode() : 0;
    }
}