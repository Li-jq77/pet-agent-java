package com.ljq.petagent.controller;

import com.ljq.petagent.entity.AppUser;
import com.ljq.petagent.entity.Deworming;
import com.ljq.petagent.entity.PetProfile;
import com.ljq.petagent.entity.Reminder;
import com.ljq.petagent.entity.Vaccination;
import com.ljq.petagent.entity.WeightRecord;
import com.ljq.petagent.repository.DewormingRepository;
import com.ljq.petagent.repository.PetProfileRepository;
import com.ljq.petagent.repository.ReminderRepository;
import com.ljq.petagent.repository.VaccinationRepository;
import com.ljq.petagent.repository.WeightRecordRepository;
import com.ljq.petagent.service.CurrentUserService;
import com.ljq.petagent.service.ViewSupportService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class PetController {

    private final CurrentUserService currentUserService;
    private final ViewSupportService viewSupportService;
    private final PetProfileRepository petRepository;
    private final WeightRecordRepository weightRepository;
    private final VaccinationRepository vaccinationRepository;
    private final DewormingRepository dewormingRepository;
    private final ReminderRepository reminderRepository;

    public PetController(
        CurrentUserService currentUserService,
        ViewSupportService viewSupportService,
        PetProfileRepository petRepository,
        WeightRecordRepository weightRepository,
        VaccinationRepository vaccinationRepository,
        DewormingRepository dewormingRepository,
        ReminderRepository reminderRepository
    ) {
        this.currentUserService = currentUserService;
        this.viewSupportService = viewSupportService;
        this.petRepository = petRepository;
        this.weightRepository = weightRepository;
        this.vaccinationRepository = vaccinationRepository;
        this.dewormingRepository = dewormingRepository;
        this.reminderRepository = reminderRepository;
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        AppUser user = currentUserService.require(authentication);
        LocalDate today = LocalDate.now();
        LocalDate weekLater = today.plusDays(7);
        List<PetProfile> myPets = petRepository.findByOwnerIdAndActiveTrueOrderByCreatedAtDesc(user.getId());
        List<Reminder> upcoming = reminderRepository
            .findByPetOwnerIdAndReminderDateBetweenAndCompletedFalseOrderByReminderDateAsc(
                user.getId(), today, weekLater
            );
        Map<LocalDate, List<Reminder>> remindersByDay = new HashMap<LocalDate, List<Reminder>>();
        for (Reminder reminder : upcoming) {
            List<Reminder> day = remindersByDay.get(reminder.getReminderDate());
            if (day == null) {
                day = new ArrayList<Reminder>();
                remindersByDay.put(reminder.getReminderDate(), day);
            }
            day.add(reminder);
        }
        List<LocalDate> weekDates = new ArrayList<LocalDate>();
        for (int i = 0; i < 7; i++) {
            weekDates.add(today.plusDays(i));
        }
        model.addAttribute("user", user);
        model.addAttribute("myPets", myPets);
        model.addAttribute("allMyPets", petRepository.findByOwnerIdOrderByIdDesc(user.getId()));
        model.addAttribute("upcomingReminders", upcoming);
        model.addAttribute("remindersByDay", remindersByDay);
        model.addAttribute("nextVaccination", reminderRepository
            .findFirstByPetOwnerIdAndReminderTypeAndReminderDateGreaterThanEqualAndCompletedFalseOrderByReminderDateAsc(
                user.getId(), "vaccination", today
            ).orElse(null));
        model.addAttribute("nextDeworming", reminderRepository
            .findFirstByPetOwnerIdAndReminderTypeAndReminderDateGreaterThanEqualAndCompletedFalseOrderByReminderDateAsc(
                user.getId(), "deworming", today
            ).orElse(null));
        model.addAttribute("weekDates", weekDates);
        model.addAttribute("petsCount", myPets.size());
        return "pets/profile";
    }

    @GetMapping("/pet/add")
    public String addForm(Model model) {
        PetProfile pet = emptyPet();
        model.addAttribute("pet", pet);
        model.addAttribute("formTitle", "添加新宠物");
        model.addAttribute("formSubtitle", "为你的毛孩子建立健康档案吧 🐾");
        model.addAttribute("submitText", "添加宠物");
        model.addAttribute("isEdit", false);
        return "pets/pet_form";
    }

    @PostMapping("/pet/add")
    public String add(
        @RequestParam Map<String, String> form,
        Authentication authentication
    ) {
        AppUser user = currentUserService.require(authentication);
        PetProfile pet = emptyPet();
        pet.setOwner(user);
        applyPetForm(pet, form);
        pet.setActive(true);
        petRepository.save(pet);
        return "redirect:/pet/" + pet.getId() + "/";
    }

    @GetMapping("/pet/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        AppUser user = currentUserService.require(authentication);
        PetProfile pet = viewSupportService.requireOwnedPet(id, user);
        List<WeightRecord> weightRecords = weightRepository.findByPetIdOrderByRecordedAtAsc(id);
        Optional<WeightRecord> latest = weightRepository.findFirstByPetIdOrderByRecordedAtDescCreatedAtDesc(id);
        pet.setLatestWeight(latest.map(WeightRecord::getWeight).orElse(null));
        List<Vaccination> vaccinations = vaccinationRepository.findByPetIdOrderByAdministeredDateDesc(id);
        List<Deworming> dewormings = dewormingRepository.findByPetIdOrderByAdministeredDateDesc(id);
        List<Reminder> allReminders = reminderRepository.findByPetIdAndCompletedFalseOrderByReminderDateAsc(id);
        List<Reminder> reminders = allReminders.size() > 10 ? allReminders.subList(0, 10) : allReminders;
        List<String> weightLabels = new ArrayList<String>();
        List<BigDecimal> weightData = new ArrayList<BigDecimal>();
        int start = Math.max(0, weightRecords.size() - 5);
        for (WeightRecord record : weightRecords.subList(start, weightRecords.size())) {
            weightLabels.add(record.getRecordedAt().toString().substring(5));
            weightData.add(record.getWeight());
        }
        model.addAttribute("pet", pet);
        model.addAttribute("weightRecords", weightRecords);
        model.addAttribute("vaccinations", vaccinations);
        model.addAttribute("dewormings", dewormings);
        model.addAttribute("reminders", reminders);
        model.addAttribute("weightChartLabels", weightLabels);
        model.addAttribute("weightChartData", weightData);
        model.addAttribute("petsCount", petRepository.countByOwnerIdAndActiveTrue(user.getId()));
        return "pets/pet_detail";
    }

    @GetMapping("/pet/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication authentication, Model model) {
        AppUser user = currentUserService.require(authentication);
        PetProfile pet = viewSupportService.requireOwnedPet(id, user);
        model.addAttribute("pet", pet);
        model.addAttribute("formTitle", "编辑 " + pet.getName() + " 的档案");
        model.addAttribute("formSubtitle", "");
        model.addAttribute("submitText", "保存修改");
        model.addAttribute("isEdit", true);
        return "pets/pet_form";
    }

    @PostMapping("/pet/{id}/edit")
    public String edit(
        @PathVariable Long id,
        @RequestParam Map<String, String> form,
        Authentication authentication
    ) {
        AppUser user = currentUserService.require(authentication);
        PetProfile pet = viewSupportService.requireOwnedPet(id, user);
        applyPetForm(pet, form);
        petRepository.save(pet);
        return "redirect:/pet/" + id + "/";
    }

    @GetMapping("/pet/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication) {
        AppUser user = currentUserService.require(authentication);
        PetProfile pet = viewSupportService.requireOwnedPet(id, user);
        petRepository.delete(pet);
        return "redirect:/profile/";
    }

    @PostMapping("/pet/{petId}/weight/add")
    public String addWeight(
        @PathVariable Long petId,
        @RequestParam String weight,
        @RequestParam(required = false) String recordedAt,
        Authentication authentication
    ) {
        AppUser user = currentUserService.require(authentication);
        PetProfile pet = viewSupportService.requireOwnedPet(petId, user);
        WeightRecord record = new WeightRecord();
        record.setPet(pet);
        record.setWeight(new BigDecimal(weight));
        record.setRecordedAt(isBlank(recordedAt) ? LocalDate.now() : LocalDate.parse(recordedAt));
        record.setNotes("");
        weightRepository.save(record);
        return "redirect:/pet/" + petId + "/";
    }

    @GetMapping("/weight/{id}/delete")
    public String deleteWeight(@PathVariable Long id, Authentication authentication) {
        AppUser user = currentUserService.require(authentication);
        WeightRecord record = weightRepository.findById(id).orElse(null);
        if (record == null || !record.getPet().getOwner().getId().equals(user.getId())) {
            return "redirect:/profile/";
        }
        Long petId = record.getPet().getId();
        weightRepository.delete(record);
        return "redirect:/pet/" + petId + "/";
    }

    @PostMapping("/pet/{petId}/vaccination/add")
    public String addVaccination(
        @PathVariable Long petId,
        @RequestParam Map<String, String> form,
        Authentication authentication
    ) {
        AppUser user = currentUserService.require(authentication);
        PetProfile pet = viewSupportService.requireOwnedPet(petId, user);
        String administeredDate = form.get("administered_date");
        if (!isBlank(administeredDate) && !isBlank(form.get("vaccine_type"))) {
            Vaccination vaccination = new Vaccination();
            vaccination.setPet(pet);
            vaccination.setVaccineType(form.get("vaccine_type"));
            vaccination.setVaccineName(value(form, "vaccine_name"));
            vaccination.setDoseNumber(parseInt(value(form, "dose_number"), 1));
            vaccination.setAdministeredDate(LocalDate.parse(administeredDate));
            vaccination.setNextDueDate(parseDate(form.get("next_due_date")));
            vaccination.setClinic(value(form, "clinic"));
            vaccination.setVeterinarian(value(form, "veterinarian"));
            vaccination.setNotes(value(form, "notes"));
            vaccinationRepository.save(vaccination);
            if (vaccination.getNextDueDate() != null) {
                Reminder reminder = new Reminder();
                reminder.setPet(pet);
                reminder.setReminderType("vaccination");
                reminder.setTitle(pet.getName() + " 的疫苗 - 第" + vaccination.getDoseNumber() + "针");
                reminder.setReminderDate(vaccination.getNextDueDate());
                reminder.setDescription("");
                reminder.setCompleted(false);
                reminderRepository.save(reminder);
            }
        }
        return "redirect:/pet/" + petId + "/";
    }

    @GetMapping("/vaccination/{id}/delete")
    public String deleteVaccination(@PathVariable Long id, Authentication authentication) {
        AppUser user = currentUserService.require(authentication);
        Vaccination vaccination = vaccinationRepository.findById(id).orElse(null);
        if (vaccination == null || !vaccination.getPet().getOwner().getId().equals(user.getId())) {
            return "redirect:/profile/";
        }
        Long petId = vaccination.getPet().getId();
        vaccinationRepository.delete(vaccination);
        return "redirect:/pet/" + petId + "/";
    }

    @PostMapping("/pet/{petId}/deworming/add")
    public String addDeworming(
        @PathVariable Long petId,
        @RequestParam Map<String, String> form,
        Authentication authentication
    ) {
        AppUser user = currentUserService.require(authentication);
        PetProfile pet = viewSupportService.requireOwnedPet(petId, user);
        String administeredDate = form.get("administered_date");
        if (!isBlank(administeredDate) && !isBlank(form.get("deworming_type"))) {
            Deworming deworming = new Deworming();
            deworming.setPet(pet);
            deworming.setDewormingType(form.get("deworming_type"));
            deworming.setProductName(value(form, "product_name"));
            deworming.setAdministeredDate(LocalDate.parse(administeredDate));
            deworming.setNextDueDate(parseDate(form.get("next_due_date")));
            deworming.setNotes("");
            dewormingRepository.save(deworming);
            if (deworming.getNextDueDate() != null) {
                Reminder reminder = new Reminder();
                reminder.setPet(pet);
                reminder.setReminderType("deworming");
                reminder.setTitle(pet.getName() + " 的驱虫");
                reminder.setReminderDate(deworming.getNextDueDate());
                reminder.setDescription("");
                reminder.setCompleted(false);
                reminderRepository.save(reminder);
            }
        }
        return "redirect:/pet/" + petId + "/";
    }

    @GetMapping("/deworming/{id}/delete")
    public String deleteDeworming(@PathVariable Long id, Authentication authentication) {
        AppUser user = currentUserService.require(authentication);
        Deworming deworming = dewormingRepository.findById(id).orElse(null);
        if (deworming == null || !deworming.getPet().getOwner().getId().equals(user.getId())) {
            return "redirect:/profile/";
        }
        Long petId = deworming.getPet().getId();
        dewormingRepository.delete(deworming);
        return "redirect:/pet/" + petId + "/";
    }

    @GetMapping("/calendar")
    public String calendar(
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) Integer month,
        Authentication authentication,
        Model model
    ) {
        AppUser user = currentUserService.require(authentication);
        LocalDate today = LocalDate.now();
        int currentYear = year == null ? today.getYear() : year;
        int currentMonth = month == null ? today.getMonthValue() : month;
        if (currentMonth < 1 || currentMonth > 12) {
            currentYear = today.getYear();
            currentMonth = today.getMonthValue();
        }
        YearMonth yearMonth = YearMonth.of(currentYear, currentMonth);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        List<Reminder> reminders = reminderRepository
            .findByPetOwnerIdAndReminderDateBetweenOrderByReminderDateAsc(user.getId(), start, end);
        List<CalendarDay> calendarDays = new ArrayList<CalendarDay>();
        for (int i = 0; i < start.getDayOfWeek().getValue() - 1; i++) {
            calendarDays.add(null);
        }
        for (int day = 1; day <= end.getDayOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            List<Reminder> dayReminders = new ArrayList<Reminder>();
            for (Reminder reminder : reminders) {
                if (reminder.getReminderDate().equals(date)) {
                    dayReminders.add(reminder);
                }
            }
            calendarDays.add(new CalendarDay(
                day,
                date,
                date.equals(today),
                date.isBefore(today),
                dayReminders
            ));
        }
        model.addAttribute("myPets", petRepository.findByOwnerIdAndActiveTrueOrderByCreatedAtDesc(user.getId()));
        model.addAttribute("reminders", reminders);
        model.addAttribute("calendarDays", calendarDays);
        model.addAttribute("curYear", currentYear);
        model.addAttribute("curMonth", currentMonth);
        model.addAttribute("curMonthName", currentMonth + "月");
        YearMonth previous = yearMonth.minusMonths(1);
        YearMonth next = yearMonth.plusMonths(1);
        model.addAttribute("prevYear", previous.getYear());
        model.addAttribute("prevMonth", previous.getMonthValue());
        model.addAttribute("nextYear", next.getYear());
        model.addAttribute("nextMonth", next.getMonthValue());
        return "pets/calendar";
    }

    @PostMapping("/pet/{petId}/reminder/add")
    public String addReminder(
        @PathVariable Long petId,
        @RequestParam Map<String, String> form,
        Authentication authentication
    ) {
        AppUser user = currentUserService.require(authentication);
        PetProfile pet = viewSupportService.requireOwnedPet(petId, user);
        if (!isBlank(form.get("title")) && !isBlank(form.get("reminder_date"))) {
            Reminder reminder = new Reminder();
            reminder.setPet(pet);
            reminder.setReminderType("custom");
            reminder.setTitle(form.get("title"));
            reminder.setDescription(value(form, "description"));
            reminder.setReminderDate(LocalDate.parse(form.get("reminder_date")));
            reminder.setRepeatIntervalDays(parseNullableInt(form.get("repeat_interval_days")));
            reminder.setCompleted(false);
            reminderRepository.save(reminder);
        }
        return "redirect:/pet/" + petId + "/";
    }

    @GetMapping("/reminder/{id}/complete")
    public String completeReminder(
        @PathVariable Long id,
        @RequestParam(defaultValue = "profile") String next,
        @RequestParam(required = false) Long pk,
        Authentication authentication
    ) {
        AppUser user = currentUserService.require(authentication);
        Reminder reminder = reminderRepository.findById(id).orElse(null);
        if (reminder == null || !reminder.getPet().getOwner().getId().equals(user.getId())) {
            return "redirect:/profile/";
        }
        reminder.setCompleted(true);
        reminder.setCompletedAt(LocalDateTime.now());
        reminderRepository.save(reminder);
        if (reminder.getRepeatIntervalDays() != null) {
            Reminder repeated = new Reminder();
            repeated.setPet(reminder.getPet());
            repeated.setReminderType(reminder.getReminderType());
            repeated.setTitle(reminder.getTitle());
            repeated.setDescription(reminder.getDescription());
            repeated.setReminderDate(reminder.getReminderDate().plusDays(reminder.getRepeatIntervalDays()));
            repeated.setRepeatIntervalDays(reminder.getRepeatIntervalDays());
            repeated.setCompleted(false);
            reminderRepository.save(repeated);
        }
        if ("pet_detail".equals(next) && pk != null) {
            return "redirect:/pet/" + pk + "/";
        }
        if ("calendar".equals(next)) {
            return "redirect:/calendar/";
        }
        return "redirect:/profile/";
    }

    @GetMapping("/reminder/{id}/delete")
    public String deleteReminder(
        @PathVariable Long id,
        @RequestParam(defaultValue = "profile") String next,
        @RequestParam(required = false) Long pk,
        Authentication authentication
    ) {
        AppUser user = currentUserService.require(authentication);
        Reminder reminder = reminderRepository.findById(id).orElse(null);
        if (reminder == null || !reminder.getPet().getOwner().getId().equals(user.getId())) {
            return "redirect:/profile/";
        }
        reminderRepository.delete(reminder);
        if ("pet_detail".equals(next) && pk != null) {
            return "redirect:/pet/" + pk + "/";
        }
        if ("calendar".equals(next)) {
            return "redirect:/calendar/";
        }
        return "redirect:/profile/";
    }

    @GetMapping("/pet/{petId}/reminder/generate")
    public String generateReminders(@PathVariable Long petId, Authentication authentication) {
        AppUser user = currentUserService.require(authentication);
        PetProfile pet = viewSupportService.requireOwnedPet(petId, user);
        LocalDate today = LocalDate.now();
        createSmartReminder(pet, "checkup", "年度体检", today.plusDays(30), 365);
        createSmartReminder(pet, "nails", "剪指甲", today.plusDays(14), 14);
        createSmartReminder(pet, "weight", "称体重", today.plusDays(7), 30);
        createSmartReminder(pet, "teeth", "刷牙", today.plusDays(3), 7);
        createSmartReminder(pet, "bath", "洗澡", today.plusDays(21), 30);
        if (!reminderRepository.existsByPetIdAndReminderType(petId, "deworming")) {
            Reminder reminder = new Reminder();
            reminder.setPet(pet);
            reminder.setReminderType("deworming");
            reminder.setTitle(pet.getName() + " 的驱虫日");
            reminder.setReminderDate(today.plusDays(60));
            reminder.setRepeatIntervalDays(90);
            reminder.setDescription("");
            reminder.setCompleted(false);
            reminderRepository.save(reminder);
        }
        return "redirect:/pet/" + petId + "/";
    }

    private void createSmartReminder(
        PetProfile pet,
        String type,
        String title,
        LocalDate date,
        int repeatDays
    ) {
        if (!reminderRepository.existsByPetIdAndReminderTypeAndReminderDate(pet.getId(), type, date)) {
            Reminder reminder = new Reminder();
            reminder.setPet(pet);
            reminder.setReminderType(type);
            reminder.setTitle(title);
            reminder.setReminderDate(date);
            reminder.setRepeatIntervalDays(repeatDays);
            reminder.setDescription("");
            reminder.setCompleted(false);
            reminderRepository.save(reminder);
        }
    }

    private PetProfile emptyPet() {
        PetProfile pet = new PetProfile();
        pet.setName("");
        pet.setSpecies("dog");
        pet.setBreed("");
        pet.setGender("male");
        pet.setAvatarUrl("");
        pet.setColor("");
        pet.setMicrochipId("");
        pet.setNotes("");
        pet.setActive(true);
        return pet;
    }

    private void applyPetForm(PetProfile pet, Map<String, String> form) {
        pet.setName(value(form, "name"));
        pet.setSpecies(value(form, "species"));
        pet.setBreed(value(form, "breed"));
        pet.setGender(value(form, "gender"));
        pet.setBirthday(parseDate(form.get("birthday")));
        pet.setAdoptedDate(parseDate(form.get("adopted_date")));
        pet.setAvatarUrl(value(form, "avatar_url"));
        pet.setColor(value(form, "color"));
        pet.setMicrochipId(value(form, "microchip_id"));
        pet.setNotes(value(form, "notes"));
        pet.setActive("true".equals(form.get("is_active")) || "on".equals(form.get("is_active")));
    }

    private String value(Map<String, String> form, String key) {
        String value = form.get(key);
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private LocalDate parseDate(String value) {
        return isBlank(value) ? null : LocalDate.parse(value);
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private Integer parseNullableInt(String value) {
        try {
            return isBlank(value) ? null : Integer.valueOf(value);
        } catch (Exception ex) {
            return null;
        }
    }

    public static class CalendarDay {
        private final int day;
        private final LocalDate date;
        private final boolean today;
        private final boolean past;
        private final List<Reminder> reminders;

        CalendarDay(int day, LocalDate date, boolean today, boolean past, List<Reminder> reminders) {
            this.day = day;
            this.date = date;
            this.today = today;
            this.past = past;
            this.reminders = reminders;
        }

        public int getDay() {
            return day;
        }

        public LocalDate getDate() {
            return date;
        }

        public boolean isToday() {
            return today;
        }

        public boolean isPast() {
            return past;
        }

        public List<Reminder> getReminders() {
            return reminders;
        }
    }
}
