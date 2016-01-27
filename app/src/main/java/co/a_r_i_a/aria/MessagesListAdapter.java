package co.a_r_i_a.aria;

import co.a_r_i_a.aria.other.Message;
import co.a_r_i_a.aria.other.SharedProperty;
import de.hdodenhof.circleimageview.CircleImageView;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

/**
 * Created by KOALA on 07/11/2015.
 */

public class MessagesListAdapter extends BaseAdapter {

    private Context context;
    private List<Message> messagesItems;

    public MessagesListAdapter(Context context, List<Message> navDrawerItems) {
        this.context = context;
        this.messagesItems = navDrawerItems;
    }

    @Override
    public int getCount() {
        return messagesItems.size();
    }

    @Override
    public Object getItem(int position) {
        return messagesItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        /**
         * The following list not implemented reusable list items as list items
         * are showing incorrect data Add the solution if you have one
         * */

        Message m = messagesItems.get(position);

        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        // Identifying the message owner
        if (messagesItems.get(position).isSelf()) {
            // message belongs to you, so load the right aligned layout
            convertView = mInflater.inflate(R.layout.list_item_message_right, null);
        } else {
            // message belongs to other person, load the left aligned layout
            convertView = mInflater.inflate(R.layout.list_item_message_left, null);
        }

        TextView txtMsg = (TextView) convertView.findViewById(R.id.txtMsg);
        CircleImageView circleImageView = (CircleImageView) convertView.findViewById(R.id.imageViewYou);

        txtMsg.setText(m.getMessage());

        if (messagesItems.get(position).isSelf())
            Picasso.with(circleImageView.getContext()).load(SharedProperty.user.image).resize(100, 100).centerCrop().into(circleImageView);
        //circleImageView.setImageURI(Uri.parse(SharedProperty.user.image));

        return convertView;
    }
}
