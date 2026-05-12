package playerhub.player.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Player {
	@Id
	private Long id;
	private String name;
	private String firstname;
	private String lastname;
	private Integer age;
	
	@Embedded
	@AttributeOverrides({
	    @AttributeOverride(name = "date", column = @Column(name = "birth_date")),
	    @AttributeOverride(name = "place", column = @Column(name = "birth_place")),
	    @AttributeOverride(name = "country", column = @Column(name = "birth_country"))
	})
	private Birth birth;
	private String nationality;
	private String height;
	private String weight;
	private Integer number;
	private String position;
	private String photo;

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getFirstname() {
		return firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public Integer getAge() {
		return age;
	}

	public Birth getBirth() {
		return birth;
	}

	public String getNationality() {
		return nationality;
	}

	public String getHeight() {
		return height;
	}

	public String getWeight() {
		return weight;
	}

	public Integer getNumber() {
		return number;
	}

	public String getPosition() {
		return position;
	}

	public String getPhoto() {
		return photo;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public void setBirth(Birth birth) {
		this.birth = birth;
	}

	public void setNationality(String nationality) {
		this.nationality = nationality;
	}

	public void setHeight(String height) {
		this.height = height;
	}

	public void setWeight(String weight) {
		this.weight = weight;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public void setPhoto(String photo) {
		this.photo = photo;
	}
}
