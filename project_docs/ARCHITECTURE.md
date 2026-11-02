# XFiles Pro — ARCHITECTURE (المعمارية الهندسية والتقنيات)

يوثق هذا الملف الهيكل المعماري والقرارات التقنية المعتمدة لمشروع **XFiles Pro** لضمان استقرار الأداء، وسهولة الصيانة، وقابلية التوسع على المدى الطويل.

---

## 1. التقنيات المعتمدة (Technology Stack)
- **لغة البرمجة:** Kotlin بنسبة 100%.
- **واجهة المستخدم:** Jetpack Compose وفق معايير Material Design 3 (M3).
- **إدارة الحالة وتدفق البيانات:** Kotlin Coroutines و StateFlow / SharedFlow.
- **إدارة التزامن في الخلفية:** Kotlin Coroutines (`Dispatchers.IO`) و WorkManager للمهام المجدولة والممتدة.
- **قواعد البيانات المحلية:** Room Database مع دعم SQLite FTS5 لفهرسة وتخزين بيانات الملفات.
- **التخزين الخفيف للإعدادات:** Jetpack DataStore (Preferences).
- **الوصول للملفات والتخزين:** Android Storage Access Framework (SAF) و MediaStore API و DocumentFile / File APIs.
- **الأمان والتشفير:** Android Keystore مع خوارزمية AES-256-GCM للبيانات وPBKDF2 للمرور.
- **أدوات البناء:** Gradle Kotlin DSL (`.gradle.kts`) مع Version Catalog (`libs.versions.toml`).

---

## 2. النمط المعماري العام (Architectural Pattern)
يعتمد المشروع معمارية **Clean Architecture** المدمجة مع **MVVM (Model-View-ViewModel)** لضمان فصل الاهتمامات واستقلالية كل طبقة:

```
┌────────────────────────────────────────────────────────┐
│                   UI Layer (Presentation)              │
│   Jetpack Compose Composables, Screens, Navigation     │
│   ViewModels (StateFlow, UiState, UiEvents)            │
└───────────────────────────▲────────────────────────────┘
                            │
┌───────────────────────────┴────────────────────────────┐
│                    Domain Layer                        │
│   Use Cases / Interactors (e.g., IndexFilesUseCase)    │
│   Domain Models (e.g., FileItem, StorageVolume)        │
│   Repository Interfaces (Contracts)                    │
└───────────────────────────▲────────────────────────────┘
                            │
┌───────────────────────────┴────────────────────────────┐
│                     Data Layer                         │
│   Repositories Implementations                         │
│   Data Sources:                                        │
│     - Room DB (IndexedFileDao, FTS5)                   │
│     - StorageEngine (SAF, File I/O, DocumentFile)      │
│     - PreferencesDataStore (Theme, Language, Settings) │
│     - KeystoreManager (Crypto/Vault)                   │
└────────────────────────────────────────────────────────┘
```

---

## 3. تفصيل الطبقات الهندسية

### أ. طبقة واجهة المستخدم (UI Layer)
- **Composables أحادية المسؤولية:** واجهات مصغرة تستقبل حالات مجردة (`State`) وتطلق أحداثاً (`Events / Lambdas`) دون ارتباط مباشر بقواعد البيانات أو ملفات النظام.
- **State Hoisting:** رفع الحالة وإدارتها مركزياً داخل الـ ViewModel لضمان إمكانية معاينة واختبار المكونات.
- **UiState:** تمثيل حالة الشاشة عبر `sealed interface` تمثل الحالات (Loading, Success, Empty, Error) لتفادي الحالات غير المحددة.
- **RTL/LTR Support:** دعم اتجاه الكتابة من اليمين لليسان للعربية والفارسية تلقائياً باستخدام إمكانيات Jetpack Compose المدمجة.

### ب. طبقة منطق الأعمال (Domain Layer)
- كود Kotlin نقي (Pure Kotlin) قدر الإمكان دون الاعتماد على مكتبات واجهة المستخدم.
- تحتوي على حالات الاستخدام (Use Cases) التي تنفذ مهمة واحدة محددة (مثل: `CopyFilesUseCase`, `SearchIndexedFilesUseCase`, `ToggleVaultLockUseCase`).
- تحدد العقود والواجهات البرمجية (Repository Interfaces) التي يجب على طبقة البيانات تلبيتها.

### ج. طبقة البيانات (Data Layer)
- **Repository Pattern:** إخفاء مصدر البيانات الحقيقي وتوفير نقطة وصول موحدة لطبقة الـ Domain.
- **Persistent Index Engine:**
  - جدول `indexed_files` في Room مع فهرس نص كامل `indexed_files_fts` مدعوم بـ FTS5.
  - دعم البحث البادئ (Prefix Search) والفرز متعدد المعايير (اسم، حجم، تاريخ، امتداد).
  - مزامنة خلفية تزايدية عبر `ContentObserver` تكتشف التعديلات وتحدث السجلات المتأثرة فقط.
- **Storage Access Layer:** معالجة آمنة لـ `Uri` عبر `ContentResolver.takePersistableUriPermission` للحفاظ على وصول دائم لبطاقات الذاكرة والمجلدات المختارة عبر SAF.
- **Security & Vault:** تشفير الملفات في تيار مباشر (Streaming Encryption) باستخدام `CipherOutputStream` لتفادي تحميل الملفات الضخمة في الذاكرة العشوائية.

---

## 4. نظام حقن الاعتماديات (Dependency Injection)
- استخدام Constructor Injection المنظم والواضح كنمط أساسي، مع تجميع التبعيات في حاويات موحدة (Service Locators / DI Modules) قابلة للربط بسهولة مع Hilt في مراحل التوسع.

---

## 5. قواعد الأداء والذاكرة (Performance & Memory Guidelines)
1. **عدم حظر الواجهة:** منع تشغيل أي استعلام لـ Room أو عملية قراءة للملفات على الـ Main Thread نهائياً.
2. **Buffer Streams:** استخدام أحجام كتل مدروسة (64KB - 128KB Buffers) عند نقل أو نسخ الملفات لتقليل زمن النقل وعدم استنزاف المعالج.
3. **Paging:** استخدام التحميل التدريجي عند تصفح المجلدات التي تحتوي على مئات أو آلاف الملفات.
4. **Thumbnail Caching:** ذاكرة تخزين مؤقت ثنائية المستوى (Memory Cache + Disk Cache) للصور المصغرة للأيقونات والوسائط.
