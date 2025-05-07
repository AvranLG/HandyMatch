package com.example.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class MensajeAdapter extends RecyclerView.Adapter<MensajeAdapter.MensajeViewHolder> {

    private List<Mensaje> listaMensajes;
    private Context context;
    private FirebaseAuth mAuth;

    public MensajeAdapter(List<Mensaje> listaMensajes, Context context) {
        this.listaMensajes = listaMensajes;
        this.context = context;
    }

    @NonNull
    @Override
    public MensajeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_mensaje, parent, false);
        return new MensajeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MensajeViewHolder holder, int position) {
        Mensaje mensaje = listaMensajes.get(position);

        mAuth = FirebaseAuth.getInstance();
        String uidAuth = mAuth.getCurrentUser().getUid();
        // Verifica si el mensaje es del emisor
        if (mensaje.getUidEmisor().equals(uidAuth)) {
            // Mensaje del emisor: alineado a la derecha, color verde
            holder.leftMessageContainer.setVisibility(View.GONE);
            holder.rightMessageContainer.setVisibility(View.VISIBLE);
            holder.rightMessageText.setText(mensaje.getMensaje());
        } else {
            // Mensaje del receptor: alineado a la izquierda, color morado
            holder.rightMessageContainer.setVisibility(View.GONE);
            holder.leftMessageContainer.setVisibility(View.VISIBLE);
            holder.leftMessageText.setText(mensaje.getMensaje());
        }
    }


    @Override
    public int getItemCount() {
        return listaMensajes.size();
    }

    public class MensajeViewHolder extends RecyclerView.ViewHolder {
        LinearLayout leftMessageContainer, rightMessageContainer;
        TextView leftMessageText, rightMessageText;

        public MensajeViewHolder(View itemView) {
            super(itemView);
            leftMessageContainer = itemView.findViewById(R.id.left_message_container);
            rightMessageContainer = itemView.findViewById(R.id.right_message_container);
            leftMessageText = itemView.findViewById(R.id.left_message_text);
            rightMessageText = itemView.findViewById(R.id.right_message_text);
        }
    }

}
