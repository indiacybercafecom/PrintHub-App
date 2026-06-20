package com.indiacybercafe.printhub;

import java.util.ArrayList;
import java.util.List;

public class ServiceRepository {

    public static List<ServiceModel> getLocalServices() {
        List<ServiceModel> list = new ArrayList<>();

        list.add(new ServiceModel(
            "All Files",
            "all_files",
            R.drawable.all_files,
            "#FFF3CD",
            true
        ));

        list.add(new ServiceModel(
            "Camera",
            "camera",
            R.drawable.camara,
            "#E3F2FD",
            true
        ));

        list.add(new ServiceModel(
            "PDF",
            "pdf",
            R.drawable.pdf,
            "#FFEBEE",
            true
        ));

        list.add(new ServiceModel(
            "Gallery",
            "gallery",
            R.drawable.gallery,
            "#F3E5F5",
            true
        ));

        list.add(new ServiceModel(
            "DOC",
            "doc",
            R.drawable.doc,
            "#E8F0FE",
            true
        ));

        list.add(new ServiceModel(
            "XLS",
            "xls",
            R.drawable.xls,
            "#E8F5E9",
            true
        ));

        list.add(new ServiceModel(
            "PPT",
            "ppt",
            R.drawable.ppt,
            "#FFF3E0",
            true
        ));

        list.add(new ServiceModel(
            "ID Card",
            "id_card",
            R.drawable.id_card,
            "#F5F5F5",
            true
        ));

        return list;
    }
}
