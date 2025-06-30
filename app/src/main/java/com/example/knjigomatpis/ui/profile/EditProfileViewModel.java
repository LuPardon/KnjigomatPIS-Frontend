package com.example.knjigomatpis.ui.profile;

import androidx.lifecycle.ViewModel;

public class EditProfileViewModel extends ViewModel {

    private String firstName;
    private String lastName;
    private String location;

    public void setUserData(String firstName, String lastName, String location) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.location = location;
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getLocation() { return location; }
}
