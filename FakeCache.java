public class FakeCache {

    private Integer viewCount = null; // Cache boshlanishda bo'sh

    public Integer get() {
        sleep(0); // tezlik uchun 0 qildim
        return viewCount;
    }

    public void set(Integer value) {
        // sleep(1); // tezlik uchun o'chirdim
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
