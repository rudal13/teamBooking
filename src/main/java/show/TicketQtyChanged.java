
package show;

public class TicketQtyChanged extends AbstractEvent {

    private Long id;
    private Long bookId;
    private Integer totalCount;
    private Integer remainCount;
    private String resultCode;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookId() {
        return bookId;
    }
    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public Integer getTotalCount() {
        return totalCount;
    }
    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getRemainCount() {
        return remainCount;
    }
    public void setRemainCount(Integer remainCount) {
        this.remainCount = remainCount;
    }

    public String getResultCode() {
        return resultCode;
    }
    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }
}
