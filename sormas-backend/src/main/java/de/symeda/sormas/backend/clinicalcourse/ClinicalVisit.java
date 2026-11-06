package de.symeda.sormas.backend.clinicalcourse;

import static de.symeda.sormas.api.utils.FieldConstraints.CHARACTER_LIMIT_DEFAULT;

import java.util.Date;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.backend.common.AbstractDomainObject;
import de.symeda.sormas.backend.symptoms.Symptoms;

@Entity
public class ClinicalVisit extends AbstractDomainObject {

	private static final long serialVersionUID = -8220449896773019721L;

	public static final String TABLE_NAME = "clinicalvisit";

	public static final String CLINICAL_COURSE = "clinicalCourse";
	public static final String SYMPTOMS = "symptoms";
	public static final String DISEASE = "disease";
	public static final String VISIT_DATE_TIME = "visitDateTime";
	public static final String VISIT_REMARKS = "visitRemarks";
	public static final String VISITING_PERSON = "visitingPerson";

	private ClinicalCourse clinicalCourse;
	private Symptoms symptoms;
	private Disease disease;
	private Date visitDateTime;
	private String visitRemarks;
	private String visitingPerson;

	@ManyToOne(cascade = {})
	@JoinColumn(nullable = false)
	public ClinicalCourse getClinicalCourse() {
		return clinicalCourse;
	}

	public void setClinicalCourse(ClinicalCourse clinicalCourse) {
		this.clinicalCourse = clinicalCourse;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	public Symptoms getSymptoms() {
		if (symptoms == null) {
			symptoms = new Symptoms();
		}
		return symptoms;
	}

	public void setSymptoms(Symptoms symptoms) {
		this.symptoms = symptoms;
	}

	@Enumerated(EnumType.STRING)
	public Disease getDisease() {
		return disease;
	}

	public void setDisease(Disease disease) {
		this.disease = disease;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getVisitDateTime() {
		return visitDateTime;
	}

	public void setVisitDateTime(Date visitDateTime) {
		this.visitDateTime = visitDateTime;
	}

	@Column(length = CHARACTER_LIMIT_DEFAULT)
	public String getVisitRemarks() {
		return visitRemarks;
	}

	public void setVisitRemarks(String visitRemarks) {
		this.visitRemarks = visitRemarks;
	}

	@Column(length = CHARACTER_LIMIT_DEFAULT)
	public String getVisitingPerson() {
		return visitingPerson;
	}

	public void setVisitingPerson(String visitingPerson) {
		this.visitingPerson = visitingPerson;
	}
}
