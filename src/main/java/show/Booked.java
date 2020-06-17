package show;

public class Booked extends AbstractEvent {

    private Long id;
    private Long showId;
    private Long showName;
    private Integer qty;
    private Integer amount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShowId() { return showId; }
    public void setShowId(Long showId) {
        this.showId = showId;
    }

    public Long getShowName() {
        return showName;
    }
    public void setShowName(Long showId) { this.showName = showName;  }

    public Integer getQty() {
        return qty;
    }
    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public Integer getAmount() {
        return amount;
    }
    public void setAmount(Integer amount) {
        this.amount = amount;
    }
}
