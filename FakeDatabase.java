public class FakeDatabase {

    private int viewCount = 0; // DB da saqlanadigan asosiy qiymat

    public int getViewCount() {
        // sun'iy ravishda sekinlashtirish (race condition aniq ko‘rinsin)
        sleep(1);
        return viewCount;
    }

    public void saveViewCount(int value) {
        sleep(1);
        this.viewCount = value;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ignore
        }
    }
}
