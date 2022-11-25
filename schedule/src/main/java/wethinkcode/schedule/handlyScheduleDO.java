package wethinkcode.schedule;

import wethinkcode.loadshed.common.transfer.SlotDO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;

public class handlyScheduleDO {

    private LocalDate LOCAL_DATE;
    private Collection<SlotDO> SLOT_DO_COLLECTION;

    public handlyScheduleDO(LocalDate localDate) {
        this.LOCAL_DATE = localDate;
    }

    public LocalDate getLOCAL_DATE() {
        return LOCAL_DATE;
    }

    public Collection<SlotDO> getSLOT_DO_COLLECTION() {
        return SLOT_DO_COLLECTION;
    }

    public handlyScheduleDO(SlotDO cx) {
        this.SLOT_DO_COLLECTION = createTimeCollection( cx );
    }

    /**
     * > This function takes a SlotDO object and returns a Collection of SlotDO objects
     *
     * @param cx The SlotDO object that is being passed in.
     * @return A collection of SlotDO objects.
     */
    private Collection<SlotDO> createTimeCollection(SlotDO cx) {
        Collection<SlotDO> yyyy = new ArrayList();
        yyyy.add(cx);
        return yyyy;
    }
}