package playerhub.player.domain;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Player {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long externalId;

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

	private String team;
	private String league;

	@Embedded
	@AttributeOverrides({
	    @AttributeOverride(name = "latitude", column = @Column(name = "location_latitude")),
	    @AttributeOverride(name = "longitude", column = @Column(name = "location_longitude"))
	})
	private Location location;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private Instant createdAt;

	public Long getId() {
		return id;
	}

	public Long getExternalId() {
		return externalId;
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

	public String getTeam() {
		return team;
	}

	public String getLeague() {
		return league;
	}

	public Location getLocation() {
		return location;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setExternalId(Long externalId) {
		this.externalId = externalId;
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

	public void setTeam(String team) {
		this.team = team;
	}

	public void setLeague(String league) {
		this.league = league;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
