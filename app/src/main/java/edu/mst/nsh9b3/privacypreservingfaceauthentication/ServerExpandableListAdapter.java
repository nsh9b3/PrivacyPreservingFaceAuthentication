package edu.mst.nsh9b3.privacypreservingfaceauthentication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

/**
 * Created by nick on 11/24/15.
 */
public class ServerExpandableListAdapter extends BaseExpandableListAdapter
{
    private Context context;
    private List<ParentServerInfo> parentList;

    public ServerExpandableListAdapter(Context context, List<ParentServerInfo> parentList)
    {
        this.context = context;
        this.parentList = parentList;
    }

    @Override
    public int getGroupCount()
    {
        return parentList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition)
    {
        return 1;
    }

    @Override
    public Object getGroup(int groupPosition)
    {
        return parentList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition)
    {
        return parentList.get(groupPosition).getServerInfo();
    }

    @Override
    public long getGroupId(int groupPosition)
    {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition)
    {
        return childPosition;
    }

    @Override
    public boolean hasStableIds()
    {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent)
    {
        ParentServerInfo parentInfo = (ParentServerInfo) getGroup(groupPosition);

        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.server_listview_parent_item, null);
        }

        TextView textViewName = (TextView) convertView.findViewById(R.id.PPFA_Options_FTPServer);
        textViewName.setText(parentInfo.getServerName());

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent)
    {
        final ParentServerInfo parentInfo = (ParentServerInfo) getGroup(groupPosition);

        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.server_listview_child_item, null);
        }

        final EditText editTextFtpAddress = (EditText) convertView.findViewById(R.id.PPFA_Options_FTPAddress);
        final EditText editTextFtpPort = (EditText) convertView.findViewById(R.id.PPFA_Options_FTPPort);
        final EditText editTextFtpUsername = (EditText) convertView.findViewById(R.id.PPFA_Options_FTPUsername);
        final EditText editTextFtpPassword = (EditText) convertView.findViewById(R.id.PPFA_Options_FTPPassword);
        Button buttonSaveSettings = (Button) convertView.findViewById(R.id.PPFA_Options_SaveServerInfo);

        editTextFtpAddress.setText(parentInfo.getFtpAddress());
        editTextFtpPort.setText(parentInfo.getFtpPort());
        editTextFtpUsername.setText(parentInfo.getFtpUsername());
        editTextFtpPassword.setText(parentInfo.getFtpPassword());

        buttonSaveSettings.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Options.setSharedPreferences(parentInfo, editTextFtpAddress.getText().toString(), editTextFtpPort.getText().toString(), editTextFtpUsername.getText().toString(), editTextFtpPassword.getText().toString());
            }
        });

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition)
    {
        return true;
    }
}
