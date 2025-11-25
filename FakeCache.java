public class FakeCache {

    private Integer viewCount = null; // Cache boshlanishda bo'sh

    public Integer get() {
        sleep(0); // I did 0 for speed.
        return viewCount;
    }

    public void set(Integer value) {
        // sleep(1); // I commented for speed.
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
