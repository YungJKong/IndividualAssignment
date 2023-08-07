package com.example.individualassignment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String KEY_RESULT_INDEX = "result_index";
    private EditText etTotalAmount, etNumPeople, etIndividualValues, etPercentageOrRatio;
    private RadioGroup radioGroup;
    private RadioButton rbEqual, rbCustom, rbPercentage, rbRatio, rbIndividual;
    private Button btnCalculate, btnStore, btnShare, btnShowResults;
    private TextView tvResult, tvIndividualValues, tvPercentageOrRatio;
    private RadioGroup customOptionsLayout;
    private static final String PREF_NAME = "BreakdownResultsPref";
    private static final String KEY_TOTAL_AMOUNT = "total_amount";
    private static final String KEY_NUM_PEOPLE = "num_people";
    private static final String KEY_BREAKDOWN = "breakdown";
    private double[] breakdown = new double[0];
    private int latestResultIndex = -1;


    private double totalAmount;
    private int numPeople;
    private double[] individualValues;
    private boolean isCustomBreakdown = false;

    private boolean verifyCustomBreakdownByAmount(double totalAmount, double[] individualAmounts) {
        double sum = 0;
        for (double amount : individualAmounts) {
            sum += amount;
        }
        return Math.abs(totalAmount - sum) < 0.01; // Allowing for small floating-point errors
    }

    private double calculateEqualBreakdown(double totalAmount, int numberOfPeople) {
        return totalAmount / numberOfPeople;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etTotalAmount = findViewById(R.id.etTotalAmount);
        etNumPeople = findViewById(R.id.etNumPeople);
        etIndividualValues = findViewById(R.id.etIndividualValues);
        radioGroup = findViewById(R.id.radioGroup);
        rbEqual = findViewById(R.id.rbEqual);
        rbCustom = findViewById(R.id.rbCustom);
        btnCalculate = findViewById(R.id.btnCalculate);
        btnStore = findViewById(R.id.btnStore);
        btnShare = findViewById(R.id.btnShare);
        btnShowResults = findViewById(R.id.btnShowResults);
        tvResult = findViewById(R.id.tvResult);
        // Find the radio buttons
        //RadioButton rbEqual = findViewById(R.id.rbEqual);
        //RadioButton rbCustom = findViewById(R.id.rbCustom);
        customOptionsLayout = findViewById(R.id.customOptionsLayout);
        rbIndividual = findViewById(R.id.rbIndividual);
        rbPercentage = findViewById(R.id.rbPercentage);
        rbRatio = findViewById(R.id.rbRatio);
        //etTotalBillAmount = findViewById(R.id.etTotalBillAmount);
        etPercentageOrRatio = findViewById(R.id.etPercentageOrRatio);
        tvIndividualValues = findViewById(R.id.tvIndividualValues);
        tvPercentageOrRatio = findViewById(R.id.tvPercentageOrRatio);
        // Set "Equal Break-Down" as the default selection
        rbEqual.setChecked(true);
        etIndividualValues.setVisibility(View.GONE);
        tvIndividualValues.setVisibility(View.GONE);
        tvPercentageOrRatio.setVisibility(View.GONE);
        customOptionsLayout.setVisibility(View.GONE);

        // Delay for a few seconds and then switch to the main layout
        /*new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Start the main activity
                Intent intent = new Intent(MainActivity.this, MainAppActivity.class);
                startActivity(intent);
                finish(); // Finish the splash activity so that it won't be shown again if the user presses the back button
            }
        }, SPLASH_DELAY);*/

        btnCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateBreakdown();
            }
        });

        breakdown = new double[numPeople];
        btnStore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCustomBreakdown && breakdown != null && totalAmount > 0 && numPeople > 0) {
                    if (breakdown.length == numPeople) {
                        storeResults(breakdown, totalAmount, numPeople);
                    } else {
                        Toast.makeText(MainActivity.this, "Calculate custom breakdown first.", Toast.LENGTH_SHORT).show();
                    }
                } else if (!isCustomBreakdown && totalAmount > 0 && numPeople > 0) {
                    // For equal breakdown, the breakdown array is already filled with equal amounts
                    storeResults(breakdown, totalAmount, numPeople);
                } else {
                    Toast.makeText(MainActivity.this, "Calculate and select a custom breakdown first.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnShowResults.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showStoredResults();
            }
        });


        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareResults();
            }
        });

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbEqual) {
                    // Show/hide views based on the selection
                    etIndividualValues.setVisibility(View.GONE);
                    customOptionsLayout.setVisibility(View.GONE);
                    etPercentageOrRatio.setVisibility(View.GONE);
                    tvIndividualValues.setVisibility(View.GONE);
                    tvPercentageOrRatio.setVisibility(View.GONE);
                } else if (checkedId == R.id.rbCustom) {
                    // Show the custom options layout and hide the EditText initially
                    etIndividualValues.setVisibility(View.GONE);
                    customOptionsLayout.setVisibility(View.VISIBLE);
                    etPercentageOrRatio.setVisibility(View.GONE);
                }
            }
        });

        customOptionsLayout.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbIndividual) {
                    etIndividualValues.setVisibility(View.VISIBLE);
                    etPercentageOrRatio.setVisibility(View.GONE);
                    tvIndividualValues.setVisibility(View.VISIBLE);
                    tvPercentageOrRatio.setVisibility(View.GONE);
                } else if (checkedId == R.id.rbPercentage) {
                    etIndividualValues.setVisibility(View.GONE);
                    etPercentageOrRatio.setVisibility(View.VISIBLE);
                    tvPercentageOrRatio.setVisibility(View.VISIBLE);
                    tvIndividualValues.setVisibility(View.GONE);
                } else {
                    etIndividualValues.setVisibility(View.GONE);
                    etPercentageOrRatio.setVisibility(View.VISIBLE);
                    tvPercentageOrRatio.setVisibility(View.VISIBLE);
                    tvIndividualValues.setVisibility(View.GONE);
                }
            }
        });

        rbIndividual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Perform the action when "By Individual Values" is selected
                // Call the function to calculate breakdown by individual values
                calculateCustomBreakdownByIndividualValues();
            }
        });

        rbPercentage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Perform the action when "By Percentage" is selected
                // Call the function to calculate breakdown by percentage
                calculateCustomBreakdownByPercentage();
            }
        });

        rbRatio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Perform the action when "By Ratio" is selected
                // Call the function to calculate breakdown by ratio
                calculateCustomBreakdownByRatio();
            }
        });

        /*radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbEqual) {
                    isCustomBreakdown = false;
                    etIndividualValues.setVisibility(View.GONE);
                } else if (checkedId == R.id.rbCustom) {
                    isCustomBreakdown = true;
                    etIndividualValues.setVisibility(View.VISIBLE);
                }
            }
        });*/
    }

    private void calculateBreakdown() {
        // Close the keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        // Retrieve input values
        try {
            totalAmount = Double.parseDouble(etTotalAmount.getText().toString());
            numPeople = Integer.parseInt(etNumPeople.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid values.", Toast.LENGTH_SHORT).show();
            return;
        }

        breakdown = new double[numPeople];
        if (rbEqual.isChecked()) {
            isCustomBreakdown = false;
            double equalAmount = calculateEqualBreakdown(totalAmount, numPeople);
            //storeResults(new double[]{equalAmount}, totalAmount, numPeople);
            tvResult.setText(String.format("Each person should pay: RM %.2f", equalAmount));
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bill Breakdown");
            builder.setMessage(String.format("Each person should pay: RM %.2f", equalAmount));
            builder.setPositiveButton("OK", null);
            builder.show();
            //breakdown = new double[numPeople];
            Arrays.fill(breakdown, equalAmount);
            return; // Exit early, no need to calculate custom breakdown
        } else if (rbCustom.isChecked()) {
            isCustomBreakdown = true;
            //etIndividualValues.setVisibility(View.VISIBLE);
            int selectedCustomOptionId = customOptionsLayout.getCheckedRadioButtonId();
            if (selectedCustomOptionId == R.id.rbIndividual) {
                //calculateCustomBreakdownByIndividualValues();
                breakdown = calculateCustomBreakdownByIndividualValues();
            } else if (selectedCustomOptionId == R.id.rbPercentage) {
                //calculateCustomBreakdownByPercentage();
                breakdown = calculateCustomBreakdownByPercentage();
            } else if (selectedCustomOptionId == R.id.rbRatio) {
                //calculateCustomBreakdownByRatio();
                breakdown = calculateCustomBreakdownByRatio();
            }
            // Store the results
            if (breakdown != null) {
                //storeResults(breakdown, totalAmount, numPeople);
            }

            // Show the breakdown results
            //showBreakdownResults(breakdown, numPeople);
            // Calculate custom breakdown by percentage/ratio (You need to implement these methods)
            // breakdown = calculateCustomBreakdownByPercentage(totalAmount, percentages);
            // or
            // breakdown = calculateCustomBreakdownByRatio(totalAmount, ratios);
        } else {
            Toast.makeText(this, "Please select a breakdown option.", Toast.LENGTH_SHORT).show();
            return;
        }
    }


    // Helper method to show Toast messages
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private double[] calculateCustomBreakdownByIndividualValues() {
        // Get the total bill amount and individual values input as strings
        String totalBillStr = etTotalAmount.getText().toString().trim();
        String individualValuesStr = etIndividualValues.getText().toString().trim();
        breakdown = new double[numPeople];
        if (!totalBillStr.isEmpty() && !individualValuesStr.isEmpty()) {
            try {
                // Parse the total bill amount from string to double
                double totalBillAmount = Double.parseDouble(totalBillStr);


                // Split the individual values using commas
                String[] individualValuesArray = individualValuesStr.split(",");

                // Validate the number of individual values matches the number of people
                int numPeople = individualValuesArray.length;
                double sumIndividualValues = 0.0;
                for (String value : individualValuesArray) {
                    sumIndividualValues += Double.parseDouble(value);
                }

                if (numPeople > 0 && sumIndividualValues == totalBillAmount) {
                    // Calculate and show the breakdown when the sum of individual values matches the total bill
                    String resultMessage = "Total Bill Amount: RM " + etTotalAmount.getText().toString() + "\n"
                            + "Total Number of People: " + numPeople + "\n\n";

                    // Calculate and show the breakdown for each person
                    for (int i = 0; i < numPeople; i++) {
                        double individualAmount = Double.parseDouble(individualValuesArray[i]);
                        breakdown[i] = Double.parseDouble(individualValuesArray[i]);
                        resultMessage += "Person " + (i + 1) + " should pay: RM " + String.format(Locale.getDefault(), "%.2f", individualAmount) + "\n";
                    }
                    tvResult.setText(resultMessage);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Bill Breakdown(individual values)");
                    builder.setMessage(resultMessage);
                    builder.setPositiveButton("OK", null);
                    builder.show();
                    return breakdown;
                } else {
                    // Show an error message if the number of individual values or the sum doesn't match the total bill
                    showToast("Invalid individual values. Please enter valid amounts.");
                }
            } catch (NumberFormatException e) {
                showToast("Invalid input. Please enter valid numbers.");
            }
        } else {
            showToast("Please enter total bill amount and individual values.");
        }
        return null;
    }

    private double[] calculateCustomBreakdownByPercentage() {
        // Get the total bill amount and individual percentages input as strings
        String totalBillStr = etTotalAmount.getText().toString().trim();
        String individualPercentagesStr = etPercentageOrRatio.getText().toString().trim();
        breakdown = new double[numPeople];

        if (!totalBillStr.isEmpty() && !individualPercentagesStr.isEmpty()) {
            try {
                // Parse the total bill amount from string to double
                double totalBillAmount = Double.parseDouble(totalBillStr);

                // Split the individual percentages using commas
                String[] individualPercentagesArray = individualPercentagesStr.split(",");

                // Validate the number of individual percentages matches the number of people
                int numPeople = individualPercentagesArray.length;
                double sumPercentages = 0.0;
                for (String percentage : individualPercentagesArray) {
                    sumPercentages += Double.parseDouble(percentage);
                }

                if (numPeople > 0 && sumPercentages == 100.0) {
                    // Calculate and show the breakdown when the sum of percentages is 100%
                    for (String percentage : individualPercentagesArray) {
                        // Calculate and show the breakdown when the sum of percentages is 100%
                        String resultMessage = "Total Bill Amount: RM " + etTotalAmount.getText().toString() + "\n"
                                + "Total Number of People: " + numPeople + "\n\n";

                        // Calculate and show the breakdown for each person
                        for (int i = 0; i < numPeople; i++) {
                            //double individualAmount = (Double.parseDouble(individualPercentagesArray[i]) / 100.0) * totalBillAmount;
                            double individualPercentage = Double.parseDouble(individualPercentagesArray[i]);
                            double individualAmount = (individualPercentage / 100.0) * totalBillAmount;
                            //breakdown[i] = (Double.parseDouble(individualPercentagesArray[i]) / 100.0) * totalBillAmount;
                            //resultMessage += "Person " + (i + 1) + " should pay: RM " + String.format(Locale.getDefault(), "%.2f", individualAmount) + "\n";
                            breakdown[i] = (individualPercentage / 100.0) * totalBillAmount;
                            resultMessage += "Person " + (i + 1) + " (" + individualPercentage + "%) should pay: RM " +
                                    String.format(Locale.getDefault(), "%.2f", individualAmount) + "\n";
                        }
                        tvResult.setText(resultMessage);
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Bill Breakdown(percentage)");
                        builder.setMessage(resultMessage);
                        builder.setPositiveButton("OK", null);
                        builder.show();
                    }
                    return breakdown;
                } else {
                    // Show an error message if the number of individual percentages or the sum doesn't equal 100%
                    showToast("Invalid individual percentages. Please enter valid percentages (sum must be 100%).");
                }
            } catch (NumberFormatException e) {
                showToast("Invalid input. Please enter valid numbers.");
            }
        } else {
            showToast("Please enter total bill amount and individual percentages.");
        }
        return null;
    }

    private double[] calculateCustomBreakdownByRatio() {
        // Get the total bill amount and individual ratios input as strings
        String totalBillStr = etTotalAmount.getText().toString().trim();
        String individualRatiosStr = etPercentageOrRatio.getText().toString().trim();

        if (!totalBillStr.isEmpty() && !individualRatiosStr.isEmpty()) {
            try {
                // Parse the total bill amount from string to double
                double totalBillAmount = Double.parseDouble(totalBillStr);

                // Split the individual ratios using commas
                String[] individualRatiosArray = individualRatiosStr.split(",");

                // Validate the number of individual ratios matches the number of people
                int numPeople = individualRatiosArray.length;
                int sumRatios = 0;
                for (String ratio : individualRatiosArray) {
                    sumRatios += Integer.parseInt(ratio);
                }
                breakdown = new double[numPeople];
                if (numPeople > 0 && sumRatios > 0) {
                    // Calculate and show the breakdown based on the ratios
                    String resultMessage = "Total Bill Amount: RM " + etTotalAmount.getText().toString() + "\n"
                            + "Total Number of People: " + numPeople + "\n\n";

                    for (int i = 0; i < numPeople; i++) {
                        //double individualAmount = (Integer.parseInt(individualRatiosArray[i]) * totalBillAmount) / sumRatios;
                        //breakdown[i] = (Integer.parseInt(individualRatiosArray[i]) * totalBillAmount) / sumRatios;
                        //resultMessage += "Person " + (i + 1) + " should pay: RM " + String.format(Locale.getDefault(),
                        // "%.2f", individualAmount) + "\n";
                        double individualRatio = Integer.parseInt(individualRatiosArray[i]);
                        double individualAmount = (individualRatio * totalBillAmount) / sumRatios;
                        breakdown[i] = (individualRatio * totalBillAmount) / sumRatios;
                        resultMessage += "Person " + (i + 1) + " (" + individualRatio + ") should pay: RM " +
                                String.format(Locale.getDefault(), "%.2f", individualAmount) + "\n";
                    }
                    tvResult.setText(resultMessage);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Bill Breakdown(ratio)");
                    builder.setMessage(resultMessage);
                    builder.setPositiveButton("OK", null);
                    builder.show();
                    return breakdown;

                } else {
                    // Show an error message if the number of individual ratios or the sum is invalid
                    showToast("Invalid individual ratios. Please enter valid ratios.");
                }
            } catch (NumberFormatException e) {
                showToast("Invalid input. Please enter valid numbers.");
            }
        } else {
            showToast("Please enter total bill amount and individual ratios.");
        }
        return null;
    }


    private void storeResults(double[] breakdown, double totalAmount, int numPeople) {
        // Implement storage logic here (SharedPreferences or SQLite database)
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        int resultIndex = sharedPreferences.getInt(KEY_RESULT_INDEX, 1);

        // Increment the result index for the next result
        resultIndex++;
        if (resultIndex > 5) {
            resultIndex = 1;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();

        String breakdownString = Arrays.toString(breakdown);
        //editor.putString(KEY_BREAKDOWN, breakdownString);
        editor.putString(KEY_BREAKDOWN + "_" + resultIndex, breakdownString);

        // Store the total amount and number of people as strings
        editor.putString(KEY_TOTAL_AMOUNT + "_" + resultIndex, String.valueOf(totalAmount));
        editor.putString(KEY_NUM_PEOPLE + "_" + resultIndex, String.valueOf(numPeople));


        editor.putInt(KEY_RESULT_INDEX, resultIndex);


        // Store the data in SharedPreferences
        //editor.putString(KEY_BREAKDOWN, breakdownString);
        //editor.putString(KEY_TOTAL_AMOUNT, String.valueOf(totalAmount));
        //editor.putString(KEY_NUM_PEOPLE, String.valueOf(numPeople));
        editor.apply();

        // Update the latestResultIndex
        latestResultIndex = resultIndex;

        showToast("Breakdown results stored successfully!");

    }


    private void showStoredResults() {
        // Get the stored results from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        StringBuilder resultMessage = new StringBuilder();
        boolean atLeastOneResultStored = false;

        // Loop through all possible stored results (assuming 5 is the maximum number of results)
        for (int i = 1; i <= 5; i++) {
            String breakdownString = sharedPreferences.getString(KEY_BREAKDOWN + "_" + i, null);
            String totalAmountString = sharedPreferences.getString(KEY_TOTAL_AMOUNT + "_" + i, null);
            String numPeopleString = sharedPreferences.getString(KEY_NUM_PEOPLE + "_" + i, null);

            // Check if any data is stored for this index
            if (breakdownString != null && totalAmountString != null && numPeopleString != null) {
                atLeastOneResultStored = true;
                // Convert the stored breakdown string to a double array
                String[] breakdownArray = breakdownString.replaceAll("[\\[\\]]", "").split(", ");
                double[] storedBreakdown = new double[breakdownArray.length];
                for (int j = 0; j < breakdownArray.length; j++) {
                    storedBreakdown[j] = Double.parseDouble(breakdownArray[j]);
                }

                // Convert totalAmountString and numPeopleString to double and int respectively
                double storedTotalAmount = Double.parseDouble(totalAmountString);
                int storedNumPeople = Integer.parseInt(numPeopleString);

                // Build the result message for this stored result
                resultMessage.append("Stored Result ").append(i).append(":\n");
                resultMessage.append("Total Bill Amount: RM ").append(String.format(Locale.getDefault(), "%.2f",
                        storedTotalAmount)).append("\n");
                resultMessage.append("Total Number of People: ").append(storedNumPeople).append("\n\n");

                if (storedBreakdown.length == 1) {
                    // Display "Each person should pay" for equal breakdown
                    double equalBreakdown = storedTotalAmount / storedNumPeople;
                    resultMessage.append("Each person should pay: RM ").append(String.format(Locale.getDefault(), "%.2f",
                            equalBreakdown)).append("\n");
                } else {
                    // Append the breakdown for each person
                    for (int j = 0; j < storedBreakdown.length; j++) {
                        if (rbPercentage.isChecked()) {
                            double individualPercentage = (storedBreakdown[j] / storedTotalAmount) * 100.0;
                            resultMessage.append("Person ").append(j + 1).append(" (").append(String.format(Locale.getDefault(), "%.2f", individualPercentage))
                                    .append("%) should pay: RM ").append(String.format(Locale.getDefault(), "%.2f", storedBreakdown[j])).append("\n");
                        } else if (rbRatio.isChecked()) {
                            double individualRatio = (storedBreakdown[j] / storedTotalAmount);
                            resultMessage.append("Person ").append(j + 1).append(" (1:").append(String.format(Locale.getDefault(), "%.2f", individualRatio))
                                    .append(") should pay: RM ").append(String.format(Locale.getDefault(), "%.2f", storedBreakdown[j])).append("\n");
                        } else {
                            resultMessage.append("Person ").append(j + 1).append(" should pay: RM ").append(String.format(Locale.getDefault(), "%.2f", storedBreakdown[j])).append("\n");
                        }
                    }
                }

                resultMessage.append("\n");
            }
        }

        if (!atLeastOneResultStored) {
            showToast("No stored results found.");
            return;
        }

        // Show the results in a pop-up dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Stored Results");
        builder.setMessage(resultMessage.toString());
        builder.setPositiveButton("OK", null);
        builder.show();
    }


    private void shareResults() {
        // Get the stored results from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String breakdownString = sharedPreferences.getString(KEY_BREAKDOWN + "_" + latestResultIndex, null);
        String totalAmountString = sharedPreferences.getString(KEY_TOTAL_AMOUNT + "_" + latestResultIndex, null);
        String numPeopleString = sharedPreferences.getString(KEY_NUM_PEOPLE + "_" + latestResultIndex, null);

        if (breakdownString == null || totalAmountString == null || numPeopleString == null) {
            showToast("No stored results found.");
            return;
        }

        // Convert the stored breakdown string to a double array
        String[] breakdownArray = breakdownString.replaceAll("[\\[\\]]", "").split(", ");
        double[] storedBreakdown = new double[breakdownArray.length];
        for (int j = 0; j < breakdownArray.length; j++) {
            storedBreakdown[j] = Double.parseDouble(breakdownArray[j]);
        }

        // Convert totalAmountString and numPeopleString to double and int respectively
        double storedTotalAmount = Double.parseDouble(totalAmountString);
        int storedNumPeople = Integer.parseInt(numPeopleString);

        // Build the result message for the latest stored result
        StringBuilder resultMessage = new StringBuilder();
        resultMessage.append("Latest Stored Result:\n");
        resultMessage.append("Total Bill Amount: RM ").append(String.format(Locale.getDefault(), "%.2f",
                storedTotalAmount)).append("\n");
        resultMessage.append("Total Number of People: ").append(storedNumPeople).append("\n\n");

        // Append the breakdown for each person
        for (int j = 0; j < storedBreakdown.length; j++) {
            //resultMessage.append("Person ").append(j + 1).append(" should pay: RM ").append(String.format(Locale.getDefault(),
             //       "%.2f", storedBreakdown[j])).append("\n");
            if (rbPercentage.isChecked()) {
                double individualPercentage = (storedBreakdown[j] / storedTotalAmount) * 100.0;
                resultMessage.append("Person ").append(j + 1).append(" (").append(String.format(Locale.getDefault(), "%.2f", individualPercentage))
                        .append("%) should pay: RM ").append(String.format(Locale.getDefault(), "%.2f", storedBreakdown[j])).append("\n");
            } else if (rbRatio.isChecked()) {
                double individualRatio = (storedBreakdown[j] / storedTotalAmount);
                resultMessage.append("Person ").append(j + 1).append(" (1:").append(String.format(Locale.getDefault(), "%.2f", individualRatio))
                        .append(") should pay: RM ").append(String.format(Locale.getDefault(), "%.2f", storedBreakdown[j])).append("\n");
            } else {
                resultMessage.append("Person ").append(j + 1).append(" should pay: RM ").append(String.format(Locale.getDefault(), "%.2f", storedBreakdown[j])).append("\n");
            }
        }

        // Copy the result message to the clipboard
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Breakdown Results", resultMessage.toString());
        clipboard.setPrimaryClip(clip);


        // Create a share intent with the results message
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Shared Breakdown Results");
        shareIntent.putExtra(Intent.EXTRA_TEXT, resultMessage.toString());

        // Start the intent chooser to let the user select an app
        Intent chooserIntent = Intent.createChooser(shareIntent, "Share via");
        if (shareIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(chooserIntent);
        } else {
            showToast("No app available to share.");
        }

    }
}

