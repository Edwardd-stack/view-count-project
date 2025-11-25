public class ViewCountService {

    private final FakeDatabase db;
    private final FakeCache cache;

    // 🔐 bitta umumiy lock obyekt
    private final Object lock = new Object();

    public ViewCountService(FakeDatabase db, FakeCache cache) {
        this.db = db;
        this.cache = cache;
    }

    // Muammoli versiya (LOCK YO'Q)
    public int increaseViewCount() {
        Integer cached = cache.get();

        if (cached == null) {
            int dbValue = db.getViewCount();
            cache.set(dbValue);
            cached = dbValue;
        }

        int newValue = cached + 1;

        db.saveViewCount(newValue);
        cache.set(newValue);

        return newValue;
    }

    // LOCK bilan tuzatilgan versiya
    public int increaseViewCountWithLock() {
        synchronized (lock) {  // bu blokka bir vaqtning o'zida faqat bitta thread kira oladi
            Integer cached = cache.get();

            if (cached == null) {
                int dbValue = db.getViewCount();
                cache.set(dbValue);
                cached = dbValue;
            }

            int newValue = cached + 1;

            db.saveViewCount(newValue);
            cache.set(newValue);

            return newValue;
        }
    }

    public int getDbValue() {
        return db.getViewCount();
    }

    public Integer getCacheValue() {
        return cache.get();
    }
}
