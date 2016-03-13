/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Servlet Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloWorld
*/

package com.smartdocs.backend;


import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MyServlet extends HttpServlet {
    static Logger Log = Logger.getLogger(MyServlet.class.getName());
    Firebase firebase;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        Log.info("SmartDoc is up!");

        //Create a new Firebase instance and subscribe on child events.
        firebase = new Firebase("https://smartdocs.firebaseio.com/");

        firebase.child("patients").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot patient : dataSnapshot.getChildren()){
                    DataSnapshot colesterolo = patient.child("colesterolo");
                    if (colesterolo.exists()){
                        int sum = 0;
                        for (DataSnapshot value : colesterolo.getChildren()) {
                            if (value.exists())
                                sum += Integer.parseInt(value.getValue().toString());
                        }
                        int value = (int) (sum / colesterolo.getChildrenCount());
                        if (value != 0){
                            Firebase c = patient.getRef().child("livelli").child("colesterolo");
                            if (value < 200){
                                c.setValue("desiderabile");
                            } else if (value >= 200 && value <= 239){
                                c.setValue("moderato");
                            } else {
                                c.setValue("a rischio");
                            }
                        }
                    } else {
                        dataSnapshot.getRef().child("colesterolo");
                    }
                    DataSnapshot peso = patient.child("peso");
                    if (peso.exists()) {
                        int sum = 0;
                        for (DataSnapshot value : peso.getChildren()) {
                            if (value.exists())
                                sum += Integer.parseInt(value.getValue().toString());
                        }
                        int value = (int) (sum / peso.getChildrenCount());
                        if (value != 0){
                            Firebase p = patient.getRef().child("livelli").child("peso");
                            if (value < 80 &&  value > 0) {
                                p.setValue("normale");
                            } else if (value >= 80 && value <= 100) {
                                p.setValue("sovrappeso");
                            } else {
                                p.setValue("obeso");
                            }
                        }
                    }
                    DataSnapshot pressione = patient.child("pressione");
                    if (pressione.exists()) {
                        int sum = 0;
                        for (DataSnapshot value : pressione.getChildren()) {
                            if (value.exists())
                                sum += Integer.parseInt(value.getValue().toString());
                        }
                        int value = (int) (sum / pressione.getChildrenCount());
                        if (value != 0){
                            Firebase p = patient.getRef().child("livelli").child("pressione");
                            if (value < 75 &&  value > 0) {
                                p.setValue("pressione bassa");
                            } else if (value >= 80 && value <= 140) {
                                p.setValue("nella norma");
                            } else {
                                p.setValue("pressione alta");
                            }
                        }
                    }
                    DataSnapshot sintomi = patient.child("sintomi");
                    ArrayList<HashMap<String, Integer>> weekMap = new ArrayList<>();
                    if (sintomi.exists() && sintomi.hasChildren()){
                        for (DataSnapshot day : sintomi.getChildren()){
                            HashMap<String, Integer> dayMap = new HashMap<>();
                            for (DataSnapshot hours : day.getChildren()){
                                for (DataSnapshot sintomo : hours.getChildren()){
                                    String name = sintomo.getKey();
                                    String comment = sintomo.getValue().toString();
                                    int count = dayMap.get(name) != null ? dayMap.get(name) : 0;
                                    count++;
                                    dayMap.put(name, count);
                                }
                            }
                            weekMap.add(dayMap);
                        }
                    }

                    // check if in a day are there more than 2 Nicturia and 3 Dispnea in a day --> dangerous (?)
                    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yy HH:mm:ss:SSSS", Locale.ITALY);
                    firebase.child("doctors/Rossi/message").removeValue();
                    for (HashMap<String, Integer> day : weekMap){
                        if (day.get("aritmie") != null && day.get("aritmie") >= 4){
                            Date today = Calendar.getInstance().getTime();
                            String date = formatter.format(today);
                            firebase.child("doctors/Rossi/message").child(date).child("paziente").setValue("DonaldDuck");
                            firebase.child("doctors/Rossi/message").child(date).child("message")
                                    .setValue("got "+day.get("aritmie") + " times Aritmie in a day.");
                            firebase.child("doctors/Rossi/message").child(date).child("priority").setValue("low");
                        }
                        if (day.get("nicturia")!= null && day.get("nicturia") >= 2 && day.get("dispnea") != null && day.get("dispnea") >= 1){
                            Date today = Calendar.getInstance().getTime();
                            String date = formatter.format(today);
                            firebase.child("doctors/Rossi/message").child(date).child("paziente").setValue("DonaldDuck");
                            firebase.child("doctors/Rossi/message").child(date).child("message").setValue(
                                    "got "+day.get("dispnea") + " times Dispnea; " + day.get("nicturia") + " times Nicturia in a day;");
                            firebase.child("doctors/Rossi/message").child(date).child("priority").setValue("high");
                        }
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }
}
