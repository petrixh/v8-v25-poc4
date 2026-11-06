package de.symeda.sormas.backend.common;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;

import de.symeda.sormas.api.common.DeletionReason;

/**
 * An extension of the {@link AbstractDomainObject} that defines data that is essential to the system.
 * This data may not be deleted from the system, e.g. because it is
 * relevant for external systems that share data with or use data from SORMAS.
 */
@MappedSuperclass
public abstract class DeletableAdo extends AbstractDomainObject {

	private static final long serialVersionUID = 6512756286608581221L;

	public static final String DELETED = "deleted";
	public static final String DELETION_REASON = "deletionReason";
	public static final String OTHER_DELETION_REASON = "otherDeletionReason";

	private boolean deleted;
	private DeletionReason deletionReason;
	private String otherDeletionReason;

	@Column
	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	@Enumerated(EnumType.STRING)
	public DeletionReason getDeletionReason() {
		return deletionReason;
	}

	public void setDeletionReason(DeletionReason deletionReason) {
		this.deletionReason = deletionReason;
	}

	@Column(columnDefinition = "text")
	public String getOtherDeletionReason() {
		return otherDeletionReason;
	}

	public void setOtherDeletionReason(String deleteOtherReason) {
		this.otherDeletionReason = deleteOtherReason;
	}
}
