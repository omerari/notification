# Nextcloud Bildirim İstemcisi (Notification Client)

Bu proje, bir Nextcloud sunucusuna bağlanarak kullanıcının bildirimlerini getiren ve listeleyen basit bir Android uygulamasıdır. Uygulama, Nextcloud'un OCS (Open Collaboration Services) API'sini kullanarak XML formatındaki verileri işler.

---

## Proje Özeti

Bu uygulama, modern Android geliştirme pratiklerini göstermek amacıyla oluşturulmuştur. Temel amacı, bir sunucudan XML tabanlı veri çekme, bu veriyi ayrıştırma (parsing) ve kullanıcı arayüzünde gösterme sürecini ortaya koymaktır.

## Kullanılan Teknolojiler ve Kütüphaneler

*   **[Kotlin](https://kotlinlang.org/)**: Ana programlama dili.
*   **[Coroutines](https://github.com/Kotlin/kotlinx.coroutines)**: Asenkron işlemleri ve arka plan görevlerini (ağ istekleri gibi) yönetmek için kullanıldı.
*   **[Retrofit](https://square.github.io/retrofit/)**: HTTP isteklerini kolayca yönetmek için kullanılan bir ağ kütüphanesi.
*   **[Retrofit SimpleXML Converter](https://github.com/square/retrofit-converters/tree/master/simplexml)**: Sunucudan gelen XML yanıtlarını Kotlin nesnelerine (POJO/data class) dönüştürmek için kullanıldı.
*   **[OkHttp Logging Interceptor](https://github.com/square/okhttp/tree/master/okhttp-logging-interceptor)**: Ağ isteklerinin ve yanıtlarının detaylarını Logcat üzerinde görmek için kullanıldı. Bu, hata ayıklama sürecini büyük ölçüde kolaylaştırır.
*   **[AndroidX Kütüphaneleri](https://developer.android.com/jetpack/androidx)**: `AppCompat`, `Core KTX` gibi temel modern Android bileşenleri.
*   **[Material Components](https://material.io/develop/android)**: Modern ve kullanıcı dostu bir arayüz tasarımı için kullanıldı.

## Kurulum ve Çalıştırma

Projeyi yerel makinenizde çalıştırmak için aşağıdaki adımları izleyin:

1.  **Projeyi Klonlayın:**
    