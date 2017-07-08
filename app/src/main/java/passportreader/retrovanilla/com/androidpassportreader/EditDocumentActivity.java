package passportreader.retrovanilla.com.androidpassportreader;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import passportreader.retrovanilla.com.androidpassportreader.data.Document;
import passportreader.retrovanilla.com.androidpassportreader.database.DatabaseHandler;

public class EditDocumentActivity
        extends AppCompatActivity
        implements View.OnClickListener {

    private EditText documentNumber;
    private EditText birthDate;
    private EditText expirationDate;
    private Button save;
    private boolean updating;

    DatabaseHandler handler;

    private DatePickerDialog birthDatePickerDialog;
    private DatePickerDialog expirationDatePickerDialog;

    private SimpleDateFormat dateFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_document);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        handler = new DatabaseHandler(getApplicationContext());

        dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.US);

        documentNumber = (EditText)findViewById(R.id.add_documentnumber);
        birthDate = (EditText)findViewById(R.id.add_birthdate);
        birthDate.setInputType(InputType.TYPE_NULL);
        expirationDate = (EditText)findViewById(R.id.add_expirationdate);
        expirationDate.setInputType(InputType.TYPE_NULL);
        save = (Button)findViewById(R.id.add_save);

        initialize();
        populateFields();
    }

    private void initialize() {
        birthDate.setOnClickListener(this);
        expirationDate.setOnClickListener(this);
        save.setOnClickListener(this);

        Calendar newCalendar = Calendar.getInstance();
        birthDatePickerDialog = new DatePickerDialog(this, new OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                birthDate.setText(dateFormatter.format(newDate.getTime()));
            }

        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

        expirationDatePickerDialog = new DatePickerDialog(this, new OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                expirationDate.setText(dateFormatter.format(newDate.getTime()));
            }

        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public void onClick(View view){
        if(view == birthDate) {
            birthDatePickerDialog.show();
        } else if(view == expirationDate) {
            expirationDatePickerDialog.show();
        } else if(view == save) {
            save();
        }
    }

    private void populateFields(){

        if(getIntent().getExtras()!=null){
            updating = true;
            Document document = (Document) getIntent().getExtras().getSerializable("document");
            birthDate.setText( document.getBirthDateString() );
            expirationDate.setText( document.getExpirationDateString() );
            documentNumber.setText(document.getDocumentNumber());

            documentNumber.setKeyListener(null);

        }

    }

    private void save(){
        SimpleDateFormat sdf_dmy = new SimpleDateFormat("dd/MM/yyyy");
        Date bDate=null;
        Date eDate=null;

        try {
            bDate = sdf_dmy.parse(birthDate.getText().toString());
            eDate = sdf_dmy.parse(expirationDate.getText().toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Document newDoc =
                new Document(documentNumber.getText().toString(), bDate, eDate);

        if(updating){
            //Updating
            handler.updateDocument(newDoc);
        }else{
            //Inserting
            handler.insertDocument(newDoc);
        }

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }
}
