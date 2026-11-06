package de.symeda.sormas.backend.vaccination;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

import de.symeda.sormas.backend.immunization.entity.Immunization;

@Entity
@Subselect("SELECT immunization_id, MAX(vaccinationdate) lastVaccinationDateValue FROM vaccination GROUP BY immunization_id")
@Synchronize("vaccination")
public class LastVaccinationDate implements Serializable {

	public static final String VACCINATION_DATE = "lastVaccinationDateValue";

	private Immunization immunization;
	private Date lastVaccinationDateValue;

	@Id
	@OneToOne
	@JoinColumn(name = "immunization_id", referencedColumnName = "id")
	public Immunization getImmunization() {
		return immunization;
	}

	public void setImmunization(Immunization immunization) {
		this.immunization = immunization;
	}

	public Date getLastVaccinationDateValue() {
		return lastVaccinationDateValue;
	}

	public void setLastVaccinationDateValue(Date vaccinationDate) {
		this.lastVaccinationDateValue = vaccinationDate;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof LastVaccinationDate)) {
			return false;
		}
		LastVaccinationDate lastVaccinationDate = (LastVaccinationDate) o;
		return Objects.equals(lastVaccinationDate.immunization, immunization)
			&& Objects.equals(lastVaccinationDate.lastVaccinationDateValue, lastVaccinationDateValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(immunization, lastVaccinationDateValue);
	}
}
