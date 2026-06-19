package com.indiacybercafe.printhub.utils;

import com.indiacybercafe.printhub.R;

public class ServiceIconMapper {

    /**
     * Maps service action keys to local drawable resources.
     * Note: 'camera' action maps to 'camara.png' found in the project drawables.
     */
    public static int getIcon(String action) {
        if (action == null) {
            return R.drawable.ic_service_placeholder;
        }

        switch (action) {
            case "all_files":
                return R.drawable.all_files;

            case "camera":
                return R.drawable.camara; // Corrected to match project file name 'camara.png'

            case "pdf":
                return R.drawable.pdf;

            case "gallery":
                return R.drawable.gallery;

            case "doc":
                return R.drawable.doc;

            case "xls":
                return R.drawable.xls;

            case "ppt":
                return R.drawable.ppt;

            case "id_card":
                return R.drawable.id_card;

            default:
                return R.drawable.ic_service_placeholder;
        }
    }
}
