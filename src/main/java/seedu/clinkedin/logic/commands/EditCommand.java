package seedu.clinkedin.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.clinkedin.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static seedu.clinkedin.logic.parser.CliSyntax.PREFIX_EMAIL;
import static seedu.clinkedin.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.clinkedin.logic.parser.CliSyntax.PREFIX_NOTE;
import static seedu.clinkedin.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.clinkedin.logic.parser.CliSyntax.PREFIX_STATUS;
import static seedu.clinkedin.logic.parser.CliSyntax.PREFIX_TAG;
import static seedu.clinkedin.model.Model.PREDICATE_SHOW_ALL_PERSONS;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import seedu.clinkedin.commons.core.Messages;
import seedu.clinkedin.commons.core.index.Index;
import seedu.clinkedin.commons.util.CollectionUtil;
import seedu.clinkedin.logic.commands.exceptions.CommandException;
import seedu.clinkedin.model.Model;
import seedu.clinkedin.model.person.Address;
import seedu.clinkedin.model.person.Email;
import seedu.clinkedin.model.person.Name;
import seedu.clinkedin.model.person.Note;
import seedu.clinkedin.model.person.Person;
import seedu.clinkedin.model.person.Phone;
import seedu.clinkedin.model.person.Status;
import seedu.clinkedin.model.person.UniqueTagTypeMap;
import seedu.clinkedin.model.person.exceptions.TagTypeNotFoundException;
import seedu.clinkedin.model.tag.TagType;
import seedu.clinkedin.model.tag.UniqueTagList;
import seedu.clinkedin.model.tag.exceptions.DuplicateTagException;
import seedu.clinkedin.model.tag.exceptions.TagNotFoundException;

/**
 * Edits the details of an existing person in the clinkedin book.
 */
public class EditCommand extends Command {

    public static final String COMMAND_WORD = "edit";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Edits the details of the person identified "
            + "by the index number used in the displayed person list. "
            + "Existing values will be overwritten by the input values.\n"
            + "Parameters: INDEX (must be a positive integer) "
            + "[" + PREFIX_NAME + "NAME] "
            + "[" + PREFIX_PHONE + "PHONE] "
            + "[" + PREFIX_EMAIL + "EMAIL] "
            + "[" + PREFIX_ADDRESS + "ADDRESS] "
            + "[" + PREFIX_TAG + "TAG] "
            + "[" + PREFIX_STATUS + "STATUS] "
            + "[" + PREFIX_NOTE + "NOTE]...\n"
            + "Example: " + COMMAND_WORD + " 1 "
            + PREFIX_PHONE + "91234567 "
            + PREFIX_EMAIL + "johndoe@example.com";

    public static final String MESSAGE_EDIT_PERSON_SUCCESS = "Edited Person: %1$s";
    public static final String MESSAGE_NOT_EDITED = "At least one field to edit must be provided.";
    public static final String MESSAGE_DUPLICATE_PERSON = "This person already exists in the clinkedin book.";

    private final Index index;
    private final EditPersonDescriptor editPersonDescriptor;

    /**
     * @param index of the person in the filtered person list to edit
     * @param editPersonDescriptor details to edit the person with
     */
    public EditCommand(Index index, EditPersonDescriptor editPersonDescriptor) {
        requireNonNull(index);
        requireNonNull(editPersonDescriptor);

        this.index = index;
        this.editPersonDescriptor = editPersonDescriptor;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Person> lastShownList = model.getFilteredPersonList();

        if (index.getZeroBased() >= lastShownList.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        }

        try {
            Person personToEdit = lastShownList.get(index.getZeroBased());
            Person editedPerson = createEditedPerson(personToEdit, editPersonDescriptor);

            if (!personToEdit.isSamePerson(editedPerson) && model.hasPerson(editedPerson)) {
                throw new CommandException(MESSAGE_DUPLICATE_PERSON);
            }

            model.setPerson(personToEdit, editedPerson);
            model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
            return new CommandResult(String.format(MESSAGE_EDIT_PERSON_SUCCESS, editedPerson));
        } catch (TagTypeNotFoundException | TagNotFoundException | DuplicateTagException e) {
            throw new CommandException(e.getMessage());
        }

    }

    /**
     * Creates and returns a {@code Person} with the details of {@code personToEdit}
     * edited with {@code editPersonDescriptor}.
     */
    private static Person createEditedPerson(Person personToEdit, EditPersonDescriptor editPersonDescriptor)
            throws TagTypeNotFoundException, TagNotFoundException, DuplicateTagException {
        assert personToEdit != null;

        Name updatedName = editPersonDescriptor.getName().orElse(personToEdit.getName());
        Phone updatedPhone = editPersonDescriptor.getPhone().orElse(personToEdit.getPhone());
        Email updatedEmail = editPersonDescriptor.getEmail().orElse(personToEdit.getEmail());
        Address updatedAddress = editPersonDescriptor.getAddress().orElse(personToEdit.getAddress());
        Map<TagType, UniqueTagList> personToEditTags = personToEdit.getTags();
        UniqueTagTypeMap original = new UniqueTagTypeMap();
        original.setTagTypeMap(personToEditTags);
        UniqueTagTypeMap toEdit = editPersonDescriptor.getOldTagTypeMap().get();
        UniqueTagTypeMap editTo = editPersonDescriptor.getNewTagTypeMap().get();

        original.removeTags(toEdit);
        original.mergeTagTypeMap(editTo);
        UniqueTagTypeMap updatedTags = original;
        Note updatednote = editPersonDescriptor.getNote().orElse(personToEdit.getNote());
        Status updatedStatus = editPersonDescriptor.getStatus().orElse(personToEdit.getStatus());

        return new Person(updatedName, updatedPhone, updatedEmail, updatedAddress, updatedTags, updatedStatus,
                updatednote);
    }

    @Override
    public boolean equals(Object other) {
        // short circuit if same object
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof EditCommand)) {
            return false;
        }

        // state check
        EditCommand e = (EditCommand) other;
        return index.equals(e.index)
                && editPersonDescriptor.equals(e.editPersonDescriptor);
    }

    /**
     * Stores the details to edit the person with. Each non-empty field value will replace the
     * corresponding field value of the person.
     */
    public static class EditPersonDescriptor {
        private Name name;
        private Phone phone;
        private Email email;
        private Address address;
        private UniqueTagTypeMap oldTagTypeMap = new UniqueTagTypeMap();
        private UniqueTagTypeMap newTagTypeMap = new UniqueTagTypeMap();
        private Status status;
        private Note note;

        public EditPersonDescriptor() {}

        /**
         * Copy constructor.
         * A defensive copy of {@code tags} is used internally.
         */
        public EditPersonDescriptor(EditPersonDescriptor toCopy) {
            setName(toCopy.name);
            setPhone(toCopy.phone);
            setEmail(toCopy.email);
            setAddress(toCopy.address);
            setOldTagTypeMap(toCopy.oldTagTypeMap);
            setNewTagTypeMap(toCopy.oldTagTypeMap);
            setStatus(toCopy.status);
            setNote(toCopy.note);
        }

        /**
         * Returns true if at least one field is edited.
         */
        public boolean isAnyFieldEdited() {
            if (!newTagTypeMap.isEmpty() || !oldTagTypeMap.isEmpty()) {
                return true;
            }
            return CollectionUtil.isAnyNonNull(name, phone, email, address, status, note);
        }

        public void setName(Name name) {
            this.name = name;
        }

        public Optional<Name> getName() {
            return Optional.ofNullable(name);
        }

        public void setPhone(Phone phone) {
            this.phone = phone;
        }

        public Optional<Phone> getPhone() {
            return Optional.ofNullable(phone);
        }

        public void setEmail(Email email) {
            this.email = email;
        }

        public Optional<Email> getEmail() {
            return Optional.ofNullable(email);
        }

        public void setAddress(Address address) {
            this.address = address;
        }

        public Optional<Address> getAddress() {
            return Optional.ofNullable(address);
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public Optional<Status> getStatus() {
            return Optional.ofNullable(status);
        }

        public void setNote(Note note) {
            this.note = note;
        }

        public Optional<Note> getNote() {
            return Optional.ofNullable(note);
        }

        /**
         * Sets {@code oldTagTypeMap} to this object's {@code oldTagTypeMap}.
         * A defensive copy of {@code oldTagTypeMap} is used internally.
         */
        public void setOldTagTypeMap(UniqueTagTypeMap oldTagTypeMap) {
            this.oldTagTypeMap = oldTagTypeMap;
        }

        /**
         * Sets {@code newTagTypeMap} to this object's {@code newTagTypeMap}.
         * A defensive copy of {@code tags} is used internally.
         */
        public void setNewTagTypeMap(UniqueTagTypeMap newTagTypeMap) {
            this.newTagTypeMap = newTagTypeMap;
        }

        /**
         * Returns an unmodifiable tag set, which throws {@code UnsupportedOperationException}
         * if modification is attempted.
         * Returns {@code Optional#empty()} if {@code tags} is null.
         */
        public Optional<UniqueTagTypeMap> getOldTagTypeMap() {
            return (oldTagTypeMap != null) ? Optional.of(oldTagTypeMap) : Optional.empty();
        }

        /**
         * Returns an unmodifiable tag set, which throws {@code UnsupportedOperationException}
         * if modification is attempted.
         * Returns {@code Optional#empty()} if {@code tags} is null.
         */
        public Optional<UniqueTagTypeMap> getNewTagTypeMap() {
            return (newTagTypeMap != null) ? Optional.of(newTagTypeMap) : Optional.empty();
        }

        @Override
        public boolean equals(Object other) {
            // short circuit if same object
            if (other == this) {
                return true;
            }

            // instanceof handles nulls
            if (!(other instanceof EditPersonDescriptor)) {
                return false;
            }

            // state check
            EditPersonDescriptor e = (EditPersonDescriptor) other;

            return getName().equals(e.getName())
                    && getPhone().equals(e.getPhone())
                    && getEmail().equals(e.getEmail())
                    && getAddress().equals(e.getAddress())
                    && getOldTagTypeMap().equals(e.getOldTagTypeMap())
                    && getNewTagTypeMap().equals(e.getNewTagTypeMap())
                    && getStatus().equals(e.getStatus())
                    && getNote().equals(e.getNote());
        }
    }
}