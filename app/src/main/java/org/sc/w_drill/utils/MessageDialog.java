package org.sc.w_drill.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import org.sc.w_drill.R;

/**
 * Created by Max on 11/15/2014.
 */
public class MessageDialog
{
    protected MessageDialog()
    {

    }

    public static final void showError(Context context, String message, final Handler handler, String title)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                if (handler != null)
                    handler.doAction();
            }
        });

        if (title != null)
            builder.setTitle(title);
        else
            builder.setTitle(R.string.txt_error);

        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setCancelable(true);
        builder.create();
        builder.show();
    }

    public static final void showError(Context context, int id, final Handler handler, String title)
    {
        String message = context.getString(id);
        showError(context, message, handler, title);
    }

    public static final void showInfo(Context context, int id, final Handler handler, String title)
    {
        String message = context.getString(id);
        showInfo(context, message, handler, title);
    }

    public static final void showInfo(Context context, String message, final Handler handler, String title)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                if (handler != null)
                    handler.doAction();
            }
        });

        if (title != null)
            builder.setTitle(title);
        else
            builder.setTitle(R.string.txt_info);
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setCancelable(true);
        builder.create();
        builder.show();
    }

    public static final void showQuestion(Context context, int id, final Handler okHandler,
                                          final Handler cancelHandler, String title)
    {
        String message = context.getString(id);
        showQuestion(context, message, okHandler, cancelHandler, title);
    }

    public static final void showQuestion(Context context, String message,
                                          final Handler okHandler,
                                          final Handler cancelHandler,
                                          String title)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                if (cancelHandler != null)
                    cancelHandler.doAction();
            }
        });

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                if (okHandler != null)
                    okHandler.doAction();
            }
        });

        if (title != null)
            builder.setTitle(title);
        else
            builder.setTitle(R.string.txt_question);

        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setCancelable(true);
        builder.create();
        builder.show();
    }

    public static interface Handler
    {
        public void doAction();
    }
}
