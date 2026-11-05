package com.gymbuddy

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.fragment.app.DialogFragment
import com.gymbuddy.databinding.DialogExerciseEditorBinding

class ExerciseEditorDialogFragment : DialogFragment() {

    private var _binding: DialogExerciseEditorBinding? = null
    private val binding get() = _binding!!

    private lateinit var exercise: Exercise
    private var onSave: ((Exercise) -> Unit)? = null
    private var draftRestSec: Int? = null

    companion object {
        private const val ARG_EXERCISE = "exercise"
        private var lastPickedSec: Int = 39

        fun newInstance(exercise: Exercise, onSave: (Exercise) -> Unit): ExerciseEditorDialogFragment {
            val fragment = ExerciseEditorDialogFragment()
            val args = Bundle()
            args.putSerializable(ARG_EXERCISE, exercise)
            fragment.arguments = args
            fragment.onSave = onSave
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exercise = arguments?.getSerializable(ARG_EXERCISE) as Exercise
        draftRestSec = exercise.restSec
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogExerciseEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.nameEditText.setText(exercise.title)
        binding.notesEditText.setText(exercise.notes)
        refreshRestButton()

        binding.restEditButton.setOnClickListener { openRestPicker() }

        binding.saveButton.setOnClickListener {
            val updatedExercise = exercise.copy(
                title = binding.nameEditText.text.toString(),
                notes = binding.notesEditText.text.toString(),
                restSec = draftRestSec
            )
            onSave?.invoke(updatedExercise)
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun refreshRestButton() {
        val override = draftRestSec
        binding.restEditButton.text = if (override != null) {
            "⏱ ${ExerciseTimers.formatMss(override)}"
        } else {
            "⏱ default"
        }
    }

    private fun openRestPicker() {
        val picker = NumberPicker(requireContext()).apply {
            minValue = 0
            maxValue = ExerciseTimers.MAX_REST_SEC / 5
            displayedValues = Array(maxValue + 1) { i -> ExerciseTimers.formatMss(i * 5) }
            value = ((draftRestSec ?: lastPickedSec).coerceIn(0, ExerciseTimers.MAX_REST_SEC) / 5)
            wrapSelectorWheel = false
        }
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Rest")
            .setView(picker)
            .setPositiveButton("Set") { _, _ ->
                val sec = picker.value * 5
                draftRestSec = sec
                lastPickedSec = sec
                refreshRestButton()
            }
            .setNeutralButton("Use default") { _, _ ->
                draftRestSec = null
                refreshRestButton()
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
