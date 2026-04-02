package com.bedirhan.cityeventmonitor;

import com.bedirhan.cityeventmonitor.service.NewsTypeClassifier;

public class TestClassifier {
    public static void main(String[] args) {
        NewsTypeClassifier classifier = new NewsTypeClassifier();
        String text = "Kocaeli Üniversitesi, sanayi iş birlikleri kapsamında önemli bir yatırımı daha hayata geçirdi. Mühendislik Fakültesi Elektrik Mühendisliği Bölümü ile ABB iş birliğinde kurulan Akıllı Dağıtım Şebekesi Araştırma Laboratuvarı düzenlenen törenle açıldı. Rektör Prof.Dr. Nuh Zafer Cantürk konuşmasında ise, Enerji sektörü, sürdürülebilirlik, verimlilik ve dijital dönüşüm gibi kavramlarla doğrudan ilişkilenen stratejik bir alan haline gelmiştir. Akıllı şebeke teknolojileri ise bu dönüşümün en kritik bileşenlerinden biridir. Bugün açılışını yaptığımız laboratuvar, öğrencilerimizin ve araştırmacılarımızın bu alanda yetkinlik kazanmasına imkân sağlayacaktır. ifadelerini kullandı. Konuşmaların ardından gerçekleştirilen kurdele kesimi ile laboratuvarın açılışı yapıldı. Program, laboratuvar gezisi, teknik bilgilendirme, toplu fotoğraf çekimi ve ikram ile sona erdi.";
        System.out.println(classifier.classify(text));
    }
}
