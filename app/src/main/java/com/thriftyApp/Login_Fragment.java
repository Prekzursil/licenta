package com.thriftyApp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Login_Fragment extends Fragment implements OnClickListener {

	private static View view;

	private static EditText emailid, password;
	private static Button loginButton;
	private static TextView signUp;
	private static CheckBox show_hide_password;
	private static LinearLayout loginLayout;
	private static Animation shakeAnimation;
	private static FragmentManager fragmentManager;
	DatabaseHelper databaseHelper;

	private FirebaseAuth mAuth;
	private GoogleSignInClient mGoogleSignInClient;

	// Add a button for Google Sign-In
	private MaterialCardView googleSignInButton;

	private static final int RC_SIGN_IN = 9001;  // Request code for Google Sign-In

	public Login_Fragment() {

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

        databaseHelper = new DatabaseHelper (getContext ());
        view = inflater.inflate(R.layout.login_layout, container, false);
		initViews();
		setListeners();

		// Initialize Firebase Auth
		mAuth = FirebaseAuth.getInstance();

		// Configure Google Sign-In
		GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestIdToken(getString(R.string.default_web_client_id)) // Make sure to add your web client ID
				.requestEmail()
				.build();

		mGoogleSignInClient = GoogleSignIn.getClient(getActivity(), gso);

		googleSignInButton = view.findViewById(R.id.googleSignInBtn);  // Add this in your layout XML
		googleSignInButton.setOnClickListener(this);

		return view;
	}

	// Initiate Views
	private void initViews() {
		fragmentManager = getActivity().getSupportFragmentManager();

		emailid = (EditText) view.findViewById(R.id.login_emailid);
		password = (EditText) view.findViewById(R.id.login_password);
		loginButton = (Button) view.findViewById(R.id.loginBtn);
		signUp = (TextView) view.findViewById(R.id.createAccount);
		show_hide_password = (CheckBox) view.findViewById(R.id.show_hide_password);
		loginLayout = (LinearLayout) view.findViewById(R.id.login_layout);

		// Load ShakeAnimation
		shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);

		// Setting text selector over text views // changes colors when clicked
		XmlResourceParser xrp = getResources().getXml(R.drawable.text_selector);
		try {
			ColorStateList csl = ColorStateList.createFromXml(getResources(), xrp);

			show_hide_password.setTextColor(csl);
			signUp.setTextColor(csl);
		} catch (Exception e) {
			Log.i("Error","Exception at Line 81 Login Fragment");
		}
	}

	// Set Listeners
	private void setListeners() {
		loginButton.setOnClickListener(this);
		signUp.setOnClickListener(this);

		// Set check listener over checkbox for showing and hiding password
		show_hide_password
				.setOnCheckedChangeListener((button, isChecked) -> {

                    // If it is checkec then show password else hide
                    // password
                    if (isChecked) {

                        show_hide_password.setText(R.string.hide_pwd);// change
                                                                        // checkbox
                                                                        // text

                        password.setInputType(InputType.TYPE_CLASS_TEXT);
                        password.setTransformationMethod(HideReturnsTransformationMethod
                                .getInstance());// show password
                    } else {
                        show_hide_password.setText(R.string.show_pwd);// change
                                                                        // checkbox
                                                                        // text

                        password.setInputType(InputType.TYPE_CLASS_TEXT
                                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        password.setTransformationMethod(PasswordTransformationMethod
                                .getInstance());// hide password

                    }

                });
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.loginBtn) {
			checkValidation();
		} else if (v.getId() == R.id.createAccount) {
			// Replace signup fragment with animation
			fragmentManager
					.beginTransaction()
					.setCustomAnimations(R.anim.right_enter, R.anim.left_out)
					.replace(R.id.frameContainer, new SignUp_Fragment(), Utils.SignUp_Fragment)
					.commit();
		} else if (v.getId() == R.id.googleSignInBtn) {
			// Trigger Google Sign-In
			signInWithGoogle();
		}
	}


	private void signInWithGoogle() {
		Intent signInIntent = mGoogleSignInClient.getSignInIntent();
		startActivityForResult(signInIntent, RC_SIGN_IN);
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
		if (requestCode == RC_SIGN_IN) {
			Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
			try {
				// Google Sign-In was successful, authenticate with Firebase
				GoogleSignInAccount account = task.getResult(ApiException.class);
				firebaseAuthWithGoogle(account);
			} catch (ApiException e) {
				// Google Sign-In failed, handle error
				Toast.makeText(getContext(), "Google Sign-In failed", Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
		AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
		mAuth.signInWithCredential(credential)
				.addOnCompleteListener(requireActivity(), task -> {
					if (task.isSuccessful()) {
						// Sign-in successful
						FirebaseUser user = mAuth.getCurrentUser();
						assert user != null;

						// Query the database to check if the user exists
						List<String> data = databaseHelper.searchPass(user.getEmail()); // Fetch data using user ID
						Contact userContact = databaseHelper.getUserByGoogleId(user.getUid());

						String googleId = userContact.getEmailId();

						Log.d("googleId", "google id:" + googleId);

						Log.d("data", data.toString());

						if (data != null && data.size() > 0) {
							Toast.makeText(getContext(), "User Found", Toast.LENGTH_SHORT).show();
							// Existing user - get userId and budget
							String userId = data.get(1);
							String budget = data.get(2);
							Log.d("userId", userId);
							Log.d("budget", budget);
							Log.d("getUserId", user.getUid());

							if (googleId.equals(user.getEmail())) {

								// Save user data in Utils
								Utils.userId = userId;
								Utils.budget = budget;

								// Navigate to the splash screen
								((MainActivity) requireActivity()).moveToSplash();
							}
						} else {
							// New user - Navigate to SignUpFragment
							Toast.makeText(getContext(), "Welcome! Please complete sign-up.", Toast.LENGTH_SHORT).show();

							// Create SignUpFragment and pass the user's data
							SignUp_Fragment signUpFragment = new SignUp_Fragment();
							Bundle bundle = new Bundle();
							bundle.putString("USER_ID", user.getUid());       // Pass user ID
							bundle.putString("USER_EMAIL", user.getEmail());  // Pass email
							bundle.putString("USER_NAME", user.getDisplayName()); // Pass name
							bundle.putString("PHONE_NUMBER", user.getPhoneNumber()); // Pass phone number
							signUpFragment.setArguments(bundle);

							// Begin the fragment transaction
							requireActivity().getSupportFragmentManager().beginTransaction()
									.replace(R.id.frameContainer, signUpFragment)
									.addToBackStack(null)
									.commit();
						}
					} else {
						// If sign-in fails, display a message to the user
						Toast.makeText(getContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();
					}
				});
	}


	private void firebaseAuthWithGoogles(GoogleSignInAccount account) {
		AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
		mAuth.signInWithCredential(credential)
				.addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        // Sign-in successful
                        FirebaseUser user = mAuth.getCurrentUser();
                        // You can now proceed with the user's data (e.g., save them in your database or update UI)
                        assert user != null;
                        Toast.makeText(getContext(), "Signed in as: " + user.getEmail(), Toast.LENGTH_SHORT).show();


						//Navigate to SignUpFragment and pass the user's data

						SignUp_Fragment signUpFragment = new SignUp_Fragment();
						Bundle bundle = new Bundle();
						bundle.putString("USER_ID", user.getUid());// Pass user ID to the fragment
						bundle.putString("USER_EMAIL", user.getEmail());// Pass email to the fragment
						bundle.putString("USER_NAME", user.getDisplayName());// Pass name to the fragment
						bundle.putString("PHONE_NUMBER", user.getPhoneNumber());// Pass photo to the fragment
						signUpFragment.setArguments(bundle);

						// Begin the fragment transaction
						requireActivity().getSupportFragmentManager().beginTransaction()
								.replace(R.id.frameContainer, signUpFragment)  // Replace with your container ID
								.addToBackStack(null)  // Add to the back stack to navigate back if needed
								.commit();

                    } else {
                        // If sign-in fails, display a message to the user
                        Toast.makeText(getContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
	}


	// Check Validation before login
	private void checkValidation() {
		// Get email id and password
		String getEmailId = emailid.getText().toString();
		String getPassword = password.getText().toString();

		// Check patter for email id
		Pattern p = Pattern.compile(Utils.regEx);

		Matcher m = p.matcher(getEmailId);

		// Check for both field is empty or not
		if (getEmailId.equals("") || getEmailId.length() == 0
				|| getPassword.equals("") || getPassword.length() == 0) {
			loginLayout.startAnimation(shakeAnimation);
			new CustomToast().Show_Toast(getActivity(), view,
					"Enter both credentials.");

		}
		// Check if email id is valid or not
		else if (!m.find())
			new CustomToast().Show_Toast(getActivity(), view,
					"Your Email Id is Invalid.");
		// Else do login and do your stuff
		else {

			String userid = emailid.getText ().toString ();
			String pass = password.getText ().toString ();
			List<String> data  = databaseHelper.searchPass(userid);
			if (data.size () == 0)
				new CustomToast().Show_Toast(getActivity(), view,
						"Hello, Sign up as user to login.");
			else {
				String actualPassword = data.get (0);
				String userId = data.get (1);
				String budget = data.get (2);
				if (actualPassword.equals (pass)) {
					Utils.userId = userId;
					Utils.budget = budget;
					((MainActivity) getActivity ( )).moveToSplash ( );
				} else {
					loginLayout.startAnimation (shakeAnimation);
					new CustomToast ( ).Show_Toast (getActivity ( ), view,
							"Username and Password does not match.");
				}
			}
		}
	}
}