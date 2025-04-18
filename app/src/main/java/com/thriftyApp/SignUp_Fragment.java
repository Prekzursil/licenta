package com.thriftyApp;


import android.content.res.ColorStateList;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SignUp_Fragment extends Fragment implements OnClickListener {
	private static View view;
	private static EditText fullName, emailId, mobileNumber,
			password, budget, confirmPassword;
	private static TextView login;
	private static Button signUpButton;
	private static CheckBox terms_conditions;
	DatabaseHelper databaseHelper;
	private String uid, userEmail, userName, phoneNumber;

	public SignUp_Fragment() {

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.signup_layout, container, false);
		 databaseHelper = new DatabaseHelper (getContext ());

		// Retrieve the email passed from the previous fragment/activity
		if (getArguments() != null) {
			uid = getArguments().getString("USER_ID");
			userEmail = getArguments().getString("USER_EMAIL");
			userName = getArguments().getString("USER_NAME");
			phoneNumber = getArguments().getString("PHONE_NUMBER");
		}

		initViews();
		setListeners();



		return view;
	}

	// Initialize all views
	private void initViews() {
		fullName = (EditText) view.findViewById(R.id.fullName);
		emailId = (EditText) view.findViewById(R.id.userEmailId);
		mobileNumber = (EditText) view.findViewById(R.id.mobileNumber);
		password = (EditText) view.findViewById(R.id.password);
		budget = (EditText) view.findViewById(R.id.budget);
		confirmPassword = (EditText) view.findViewById(R.id.confirmPassword);
		signUpButton = (Button) view.findViewById(R.id.signUpBtn);
		login = (TextView) view.findViewById(R.id.already_user);
		terms_conditions = (CheckBox) view.findViewById(R.id.terms_conditions);

		emailId.setText(userEmail);
		fullName.setText(userName);
		mobileNumber.setText(phoneNumber);
		password.setText(uid);
		confirmPassword.setText(uid);

		// Setting text selector over textviews
		XmlResourceParser xrp = getResources().getXml(R.drawable.text_selector);
		try {
			ColorStateList csl = ColorStateList.createFromXml(getResources(),
					xrp);

			login.setTextColor(csl);
			terms_conditions.setTextColor(csl);
		} catch (Exception e) {
		}
	}

	// Set Listeners
	private void setListeners() {
		signUpButton.setOnClickListener(this);
		login.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.signUpBtn) {
			// Call checkValidation method
			checkValidation();
		} else if (v.getId() == R.id.already_user) {
			// Replace login fragment
			new MainActivity().replaceLoginFragment();
		}
	}


	// Check Validation Method
	private void checkValidation() {

		// Get all edittext texts
		String getFullName = fullName.getText().toString();
		String getEmailId = emailId.getText().toString();
		String getMobileNumber = mobileNumber.getText().toString();
		String getBudget = budget.getText().toString();
		String getPassword = password.getText().toString();
		String getConfirmPassword = confirmPassword.getText().toString();

		// Pattern match for email id
		Pattern p = Pattern.compile(Utils.regEx);
		Matcher m = p.matcher(getEmailId);

		// Check if all strings are null or not
		if (getFullName.isEmpty() || getFullName.isEmpty()
				|| getEmailId.isEmpty() || getEmailId.isEmpty()
				|| getMobileNumber.isEmpty() || getMobileNumber.isEmpty()
				|| getBudget.isEmpty() || getBudget.isEmpty()
				|| getPassword.isEmpty() || getPassword.isEmpty()
				|| getConfirmPassword.isEmpty()
				|| getConfirmPassword.isEmpty())

			new CustomToast().Show_Toast(getActivity(), view,
					"All fields are required.");

		// Check if email id valid or not
		else if (!m.find())
			new CustomToast().Show_Toast(getActivity(), view,
					"Your Email Id is Invalid.");

		// Check if both password should be equal
		else if (!getConfirmPassword.equals(getPassword))
			new CustomToast().Show_Toast(getActivity(), view,
					"Both password doesn't match.");

		// Make sure user should check Terms and Conditions checkbox
		else if (!terms_conditions.isChecked())
			new CustomToast().Show_Toast(getActivity(), view,
					"Please select Terms and Conditions.");

		// Else do signup or do your stuff
		else {
			Contact c = new Contact ();
			c.setName (getFullName);
			c.setEmailId (getEmailId);
			c.setMobile (Long.parseLong (getMobileNumber));
			c.setPassword (getPassword);
			c.setBudget(Long.parseLong (getBudget));
			databaseHelper.insertContact(c,uid);
			Toast.makeText(getActivity(), "Login with Email ID and password.", Toast.LENGTH_SHORT).show();
			new MainActivity().replaceLoginFragment();
		}

	}
}
