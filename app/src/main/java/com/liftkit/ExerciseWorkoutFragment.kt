package com.liftkit

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.liftkit.databinding.FragmentExerciseWorkoutBinding

class ExerciseWorkoutFragment : Fragment(), SetsEditorDialogFragment.SetsEditorListener, RepsEditorDialogFragment.RepsEditorListener, WeightEditorDialogFragment.WeightEditorListener {

    private var _binding: FragmentExerciseWorkoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var exercise: Exercise
    private var position: Int = 0
    private lateinit var onSetCompleted: (Int) -> Unit
    /** position, exercise, oldCompletedSets, newCompletedSets */
    private lateinit var onUpdate: (Int, Exercise, Int, Int) -> Unit

    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var goRunnable: Runnable? = null
    private var isTimerRunning = false
    private var showingGo: Boolean = false

    companion object {
        private const val ARG_EXERCISE = "exercise"
        private const val ARG_POSITION = "position"
        private const val GO_HOLD_MS = 900L

        fun newInstance(exercise: Exercise, position: Int): ExerciseWorkoutFragment {
            val fragment = ExerciseWorkoutFragment()
            val args = Bundle()
            args.putSerializable(ARG_EXERCISE, exercise)
            args.putInt(ARG_POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }

    fun setOnSetCompletedListener(listener: (Int) -> Unit) {
        onSetCompleted = listener
    }

    fun setOnUpdateListener(listener: (Int, Exercise, Int, Int) -> Unit) {
        onUpdate = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exercise = arguments?.getSerializable(ARG_EXERCISE) as Exercise
        position = arguments?.getInt(ARG_POSITION) ?: 0
        exercise.rating = ExerciseTimers.normalizeRating(exercise.rating)
        if (exercise.restPace.isBlank()) {
            exercise.restPace = ExerciseTimers.paceFromRating(exercise.rating)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateUI()

        bindEffort(binding.effortTrivial, "trivial")
        bindEffort(binding.effortEasy, "easy")
        bindEffort(binding.effortSolid, "solid")
        bindEffort(binding.effortHard, "hard")
        bindEffort(binding.effortUnachievable, "unachievable")
        bindPace(binding.restShort, "short")
        bindPace(binding.restMedium, "medium")
        bindPace(binding.restLong, "long")

        // Resume timer if it was running
        if (exercise.isTimerActive && exercise.timerEndTime > System.currentTimeMillis() && !isTimerRunning) {
            resumeTimer()
        }

        binding.titleText.setOnClickListener {
            val dialog = ExerciseEditorDialogFragment.newInstance(exercise) { updatedExercise ->
                val oldCompleted = exercise.completedSets
                exercise.title = updatedExercise.title
                exercise.notes = updatedExercise.notes
                updateUI()
                onUpdate(position, exercise, oldCompleted, exercise.completedSets)
            }
            dialog.show(parentFragmentManager, "exercise_editor")
        }

        binding.weightValue.setOnClickListener {
            val dialog = WeightEditorDialogFragment.newInstance(exercise.weight)
            dialog.setTargetFragment(this, 0)
            dialog.show(parentFragmentManager, "weight_editor")
        }

        binding.repsValue.setOnClickListener {
            val dialog = RepsEditorDialogFragment.newInstance(exercise.reps)
            dialog.setTargetFragment(this, 0)
            dialog.show(parentFragmentManager, "reps_editor")
        }

        binding.setsValue.setOnClickListener {
            val dialog = SetsEditorDialogFragment.newInstance(exercise.sets)
            dialog.setTargetFragment(this, 0)
            dialog.show(parentFragmentManager, "sets_editor")
        }

        binding.notesIcon.setOnClickListener { showFullScreenNotes() }

        // Set up tap listener for notes magnification toggle
        binding.notesText.setOnClickListener {
            if (binding.fullScreenNotesText.visibility == View.VISIBLE) {
                hideFullScreenNotes()
            } else {
                showFullScreenNotes()
            }
        }

        // Allow tapping full-screen notes to return to normal view
        binding.fullScreenNotesText.setOnClickListener {
            hideFullScreenNotes()
        }

        // Set up help icon click listener for Google search
        binding.helpIcon.setOnClickListener {
            searchExerciseOnline(exercise.title)
        }

        binding.progressPieChart.setOnClickListener {
            if (exercise.completedSets >= exercise.sets) {
                // Reset progress
                val oldCompleted = exercise.completedSets
                exercise.completedSets = 0
                updateUI()
                onUpdate(position, exercise, oldCompleted, exercise.completedSets)
                stopTimer()
            } else {
                // Complete set logic
                if (exercise.completedSets < exercise.sets) {
                    // Haptic feedback
                    val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        vibrator.vibrate(50)
                    }

                    val oldCompleted = exercise.completedSets
                    exercise.completedSets++
                    updateUI()
                    onUpdate(position, exercise, oldCompleted, exercise.completedSets)
                    onSetCompleted(position)
                    showSetCompletedBubble()

                    // Handle timer: stop if final set completed, otherwise start new timer
                    if (exercise.completedSets >= exercise.sets) {
                        stopTimer()  // Stop any running timer when final set is completed
                    } else {
                        val resolved = exercise.resolvedRest()
                        exercise.restSourceLabel = resolved.sourceLabel
                        startTimer(resolved.seconds)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        paintChips()
    }

    private fun startTimer(initialSeconds: Int = 59) {
        cancelTimerCallbacks()
        showingGo = false
        exercise.isTimerActive = true
        exercise.timerDurationSeconds = initialSeconds
        exercise.remainingSeconds = initialSeconds
        exercise.timerEndTime = System.currentTimeMillis() + initialSeconds * 1000L
        isTimerRunning = true
        updateUI()
        onUpdate(position, exercise, exercise.completedSets, exercise.completedSets)
        ExerciseWidgetTicker.start(requireContext())

        timerRunnable = Runnable {
            try {
                val currentTime = System.currentTimeMillis()
                exercise.remainingSeconds = maxOf(0, ((exercise.timerEndTime - currentTime) / 1000).toInt())
                if (exercise.remainingSeconds > 0) {
                    if (_binding != null) updateUI()
                    onUpdate(position, exercise, exercise.completedSets, exercise.completedSets)
                    handler.postDelayed(timerRunnable!!, 1000)
                } else {
                    if (isAdded) {
                        onRestZero()
                    }
                }
            } catch (e: Exception) {
                if (isAdded) {
                    stopTimer()
                }
            }
        }
        handler.postDelayed(timerRunnable!!, 1000)
    }

    private fun onRestZero() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
        isTimerRunning = false
        exercise.remainingSeconds = 0
        exercise.isTimerActive = false
        exercise.timerEndTime = 0
        exercise.timerDurationSeconds = 0
        showingGo = true
        try {
            val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(120)
            }
        } catch (_: Exception) {
        }
        updateUI()
        onUpdate(position, exercise, exercise.completedSets, exercise.completedSets)
        goRunnable?.let { handler.removeCallbacks(it) }
        goRunnable = Runnable {
            goRunnable = null
            if (showingGo) dismissGo()
        }
        handler.postDelayed(goRunnable!!, GO_HOLD_MS)
    }

    private fun dismissGo() {
        showingGo = false
        exercise.isTimerActive = false
        exercise.timerEndTime = 0
        exercise.timerDurationSeconds = 0
        if (_binding != null) {
            binding.progressPieChart.setCenterCountdown(null)
        }
        updateUI()
        onUpdate(position, exercise, exercise.completedSets, exercise.completedSets)
    }

    private fun resumeTimer() {
        if (exercise.isTimerActive && exercise.timerEndTime > System.currentTimeMillis()) {
            isTimerRunning = true
            updateUI()

            timerRunnable = Runnable {
                try {
                    val currentTime = System.currentTimeMillis()
                    exercise.remainingSeconds = maxOf(0, ((exercise.timerEndTime - currentTime) / 1000).toInt())
                    if (exercise.remainingSeconds > 0) {
                        if (_binding != null) updateUI()
                        onUpdate(position, exercise, exercise.completedSets, exercise.completedSets)
                        handler.postDelayed(timerRunnable!!, 1000)
                    } else {
                        if (isAdded) {
                            onRestZero()
                        }
                    }
                } catch (e: Exception) {
                    if (isAdded) {
                        stopTimer()
                    }
                }
            }
            handler.postDelayed(timerRunnable!!, 1000)
        } else if (exercise.isTimerActive && exercise.remainingSeconds <= 0) {
            onRestZero()
        }
    }

    private fun cancelTimerCallbacks() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        goRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
        goRunnable = null
    }

    private fun stopTimer() {
        cancelTimerCallbacks()
        isTimerRunning = false
        showingGo = false
        exercise.isTimerActive = false
        exercise.remainingSeconds = 0
        exercise.timerEndTime = 0
        exercise.timerDurationSeconds = 0
        updateUI()
        onUpdate(position, exercise, exercise.completedSets, exercise.completedSets)
    }

    private fun showFullScreenNotes() {
        val notesText = exercise.notes.removePrefix("Notes: ").trim()
        if (notesText.isEmpty()) return
        binding.fullScreenNotesText.text = notesText

        // Hide all other UI elements
        binding.titleContainer.visibility = View.GONE
        binding.statsRow.visibility = View.GONE
        binding.controlsBlock.visibility = View.GONE
        binding.notesText.visibility = View.GONE
        binding.notesIcon.visibility = View.GONE
        binding.progressPieChart.visibility = View.GONE

        // Show full screen notes
        binding.fullScreenNotesText.visibility = View.VISIBLE
    }

    private fun hideFullScreenNotes() {
        // Hide full screen notes
        binding.fullScreenNotesText.visibility = View.GONE

        // Show all other UI elements
        binding.titleContainer.visibility = View.VISIBLE
        binding.statsRow.visibility = View.VISIBLE
        binding.controlsBlock.visibility = View.VISIBLE
        binding.notesText.visibility = View.VISIBLE
        binding.progressPieChart.visibility = View.VISIBLE

        paintChips()
        updateUI()
    }

    private fun bindEffort(view: TextView, token: String) {
        view.setOnLongClickListener {
            val rating = ExerciseTimers.normalizeRating(token)
            if (exercise.rating != rating) {
                exercise.rating = rating
                paintChips()
                showRatingBubble(rating)
                onUpdate(position, exercise, exercise.completedSets, exercise.completedSets)
            }
            true
        }
    }

    private fun bindPace(view: TextView, pace: String) {
        view.setOnLongClickListener {
            val already = exercise.restPace == pace && exercise.restSec == null
            if (!already) {
                exercise.restPace = pace
                exercise.restSec = null
                val resolved = exercise.resolvedRest()
                exercise.restSourceLabel = resolved.sourceLabel
                paintChips()
                if (isTimerRunning) {
                    startTimer(resolved.seconds)
                } else {
                    onUpdate(position, exercise, exercise.completedSets, exercise.completedSets)
                }
            }
            true
        }
    }

    private fun paintChips() {
        if (_binding == null) return
        val rating = ExerciseTimers.normalizeRating(exercise.rating)
        styleEffortChip(binding.effortTrivial, "trivial", rating == "trivial")
        styleEffortChip(binding.effortEasy, "easy", rating == "easy")
        styleEffortChip(binding.effortSolid, "solid", rating == "solid")
        styleEffortChip(binding.effortHard, "hard", rating == "hard")
        styleEffortChip(binding.effortUnachievable, "unachievable", rating == "unachievable")

        val pace = exercise.effectivePace()
        styleChip(binding.restShort, pace == "short")
        styleChip(binding.restMedium, pace == "medium")
        styleChip(binding.restLong, pace == "long")
        val defaults = RestPrefs.defaults
        binding.restShort.text = "Short · ${ExerciseTimers.formatMss(defaults.easy)}"
        binding.restMedium.text = "Medium · ${ExerciseTimers.formatMss(defaults.good)}"
        binding.restLong.text = "Long · ${ExerciseTimers.formatMss(defaults.hard)}"
        binding.restHeading.text = if (exercise.restSec != null) {
            "Rest (long press) · custom ${ExerciseTimers.formatMss(exercise.restSec ?: 0)}"
        } else {
            "Rest (long press)"
        }
    }

    /** Trivial green, Easy yellow, Solid purple, Hard orange, Unachievable red. */
    private fun effortFill(token: String): Int = when (token) {
        "trivial" -> 0xFF3DDC84.toInt()
        "easy" -> 0xFFF9F72E.toInt()
        "solid" -> 0xFF9B59D0.toInt()
        "hard" -> 0xFFFF9800.toInt()
        else -> 0xFFE53935.toInt()
    }

    private fun textOnFill(token: String): Int = when (token) {
        "solid", "unachievable" -> 0xFFFFFFFF.toInt()
        else -> 0xFF1A1606.toInt()
    }

    private fun styleEffortChip(view: TextView, token: String, selected: Boolean) {
        val density = resources.displayMetrics.density
        view.background = GradientDrawable().apply {
            cornerRadius = 8f * density
            setColor(if (selected) effortFill(token) else 0xFF2A2A2A.toInt())
        }
        view.setTextColor(if (selected) textOnFill(token) else 0xFFBDBDBD.toInt())
        view.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun styleChip(view: TextView, selected: Boolean) {
        val density = resources.displayMetrics.density
        view.background = GradientDrawable().apply {
            cornerRadius = 8f * density
            setColor(if (selected) 0xFFF9F72E.toInt() else 0xFF2A2A2A.toInt())
        }
        view.setTextColor(if (selected) 0xFF1A1606.toInt() else 0xFFBDBDBD.toInt())
        view.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun showRatingBubble(rating: String) {
        val emoji = when (rating) {
            "trivial" -> "😌"
            "easy" -> "😊"
            "hard" -> "🥵"
            "unachievable" -> "🤯"
            else -> "💪"
        }

        // Create floating TextView
        val bubbleView = TextView(requireContext()).apply {
            text = emoji
            textSize = 48f
            alpha = 1f
        }

        // Add to the root view
        val rootView = requireActivity().findViewById<ViewGroup>(android.R.id.content)
        rootView.addView(bubbleView)

        // Position at bottom center initially
        val displayMetrics = resources.displayMetrics
        val startX = (displayMetrics.widthPixels / 2f) - 50f
        val startY = displayMetrics.heightPixels - 200f
        val endY = 100f

        bubbleView.x = startX
        bubbleView.y = startY

        // Create combined animation: upward movement with side-to-side oscillation
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000 // 2 seconds
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float

                // Upward movement
                bubbleView.y = startY - (startY - endY) * progress

                // Side-to-side oscillation (sine wave)
                val oscillation = kotlin.math.sin(progress * 4 * Math.PI).toFloat() * 30f
                bubbleView.x = startX + oscillation

                // Fade out as it goes up
                bubbleView.alpha = 1f - progress
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    rootView.removeView(bubbleView)
                }
            })
        }

        animator.start()
    }

    private fun showSetCompletedBubble() {
        val rootView = requireActivity().findViewById<ViewGroup>(android.R.id.content)
        val displayMetrics = resources.displayMetrics
        val baseStartX = (displayMetrics.widthPixels / 2f) - 50f
        val startY = displayMetrics.heightPixels - 200f
        val endY = 100f

        // Create one emoji bubble for each completed set
        for (i in 0 until exercise.completedSets) {
            // Create floating TextView with clapping hands emoji
            val bubbleView = TextView(requireContext()).apply {
                text = "👏"
                textSize = 48f
                alpha = 1f
            }

            // Add to the root view
            rootView.addView(bubbleView)

            // Position with slight offset for each bubble
            val offsetX = (i - (exercise.completedSets - 1) / 2f) * 60f // Spread bubbles horizontally
            val startX = baseStartX + offsetX

            bubbleView.x = startX
            bubbleView.y = startY

            // Create combined animation: upward movement with side-to-side oscillation
            val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 2000 // 2 seconds
                startDelay = i * 100L // Stagger start times slightly
                addUpdateListener { animation ->
                    val progress = animation.animatedValue as Float

                    // Upward movement
                    bubbleView.y = startY - (startY - endY) * progress

                    // Side-to-side oscillation (sine wave)
                    val oscillation = kotlin.math.sin(progress * 4 * Math.PI).toFloat() * 30f
                    bubbleView.x = startX + oscillation

                    // Fade out as it goes up
                    bubbleView.alpha = 1f - progress
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        rootView.removeView(bubbleView)
                    }
                })
            }

            animator.start()
        }
    }

    private fun updateUI() {
        if (_binding == null) return

        binding.titleText.text = exercise.title

        binding.weightLabel.text = "Weight"
        binding.weightValue.text = if (exercise.weight == 0) "-" else exercise.weight.toString()

        binding.repsLabel.text = "Reps"
        binding.repsValue.text = exercise.reps.toString()

        binding.setsLabel.text = "Sets"
        binding.setsValue.text = exercise.sets.toString()

        if (showingGo) {
            binding.progressPieChart.setCenterCountdown("Go")
        } else if (exercise.isTimerActive && isTimerRunning && exercise.remainingSeconds > 0) {
            binding.progressPieChart.setCenterCountdown(ExerciseTimers.formatMss(exercise.remainingSeconds))
        } else {
            binding.progressPieChart.setCenterCountdown(null)
        }
        paintChips()

        val noteBody = exercise.notes.removePrefix("Notes: ").trim()
        binding.notesText.text = if (noteBody.isNotEmpty()) "Notes: $noteBody" else ""
        val showingNote = binding.fullScreenNotesText.visibility == View.VISIBLE
        binding.notesIcon.visibility = if (!showingNote && noteBody.isNotEmpty()) View.VISIBLE else View.GONE

        binding.progressPieChart.setProgress(exercise.completedSets, exercise.sets)
        binding.progressPieChart.setSegmented(true)
        binding.progressPieChart.setSegmentDividers(true)
    }

    override fun onWeightUpdated(newWeight: Int) {
        exercise.weight = newWeight
        updateUI()
        onUpdate(position, exercise, exercise.completedSets, exercise.completedSets)
    }

    override fun onRepsUpdated(newReps: Int) {
        exercise.reps = newReps
        updateUI()
        onUpdate(position, exercise, exercise.completedSets, exercise.completedSets)
    }

    override fun onSetsUpdated(newSets: Int) {
        exercise.sets = newSets
        if (exercise.completedSets >= exercise.sets) {
            stopTimer()
        }
        updateUI()
        onUpdate(position, exercise, exercise.completedSets, exercise.completedSets)
    }

    private fun searchExerciseOnline(exerciseName: String) {
        try {
            // Create Google search URL for the exercise
            val searchQuery = Uri.encode("$exerciseName exercise")
            val searchUrl = "https://www.google.com/search?q=$searchQuery"

            // Create intent to open browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback: try to open Google in browser
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                startActivity(intent)
            } catch (e2: Exception) {
                // If all else fails, show a toast
                android.widget.Toast.makeText(
                    requireContext(),
                    "Unable to open browser",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        cancelTimerCallbacks()
        isTimerRunning = false
        super.onDestroyView()
        _binding = null
    }
}
