package passportreader.retrovanilla.com.androidpassportreader.data;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import passportreader.retrovanilla.com.androidpassportreader.R;

/**
 * Created by antonio on 16/12/15.
 */
public class DocumentArrayAdapter extends ArrayAdapter<Document> {

    private SimpleDateFormat sdf;
    private ArrayList<Document> document_list; //List of documents
    private Context adapterContext;
    private int layout_id;

    //Item holder
    static class ListItemHolder{
        TextView expirationDate;
        TextView birthDate;
        TextView documentNumber;
    }

    public DocumentArrayAdapter(Context context, int resource, ArrayList<Document> items) {
        super(context, resource, items);

        this.document_list = items;
        this.layout_id = resource;
        this.adapterContext = context;
        this.sdf = new SimpleDateFormat("dd/MM/yy");
    }

    public Document getDocument(int position){
        return document_list.get(position);
    }

    /**
     * Return a reference to this adapter list of items
     * @return The list of items shown on the listView
     */
    public ArrayList<Document> getList(){
        return this.document_list;
    }

    /**
     * Add new Document item into the list
     * @param newDocument: new Document object to be inserted in the list
     */
    public void  addItem(Document newDocument){
        this.document_list.add(newDocument);
    }

    /**
     * Fill the adapter list with some Document instances
     * @param documentList: list with Documents to be displayed in the ListView
     */
    public void populateList(ArrayList<Document> documentList){
        this.document_list = documentList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View listItem = convertView;
        ListItemHolder itemHolder;


        if(listItem == null){
            //ListItem has not been previously created
            LayoutInflater inflater =
                    (LayoutInflater) adapterContext.getApplicationContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            listItem = inflater.inflate(layout_id, null, false);

            itemHolder=new ListItemHolder();

            itemHolder.birthDate = (TextView) listItem.findViewById(R.id.item_birth_date);
            itemHolder.expirationDate = (TextView)listItem.findViewById(R.id.item_expiration_date);
            itemHolder.documentNumber = (TextView)listItem.findViewById(R.id.item_document_number);

            listItem.setTag(itemHolder);


        }else{
            //ListElement has been previously created
            itemHolder = (ListItemHolder)listItem.getTag();
        }

        itemHolder.birthDate
                .setText("Birth Date: " + sdf.format(document_list.get(position).getBirthDate()));
        itemHolder.expirationDate
                .setText("Expiration Date: " + sdf.format(document_list.get(position).getExpirationDate()));
        itemHolder.documentNumber
                .setText("ID: " + document_list.get(position).getDocumentNumber());


        return listItem;
    }
}
