package com.gymbuddy

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.gymbuddy.databinding.FragmentExerciseWorkoutBinding

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
    private var flashRunnable: Runnable? = null
    private var isTimerRunning = false
    private var nextTitle: String? = null
    private var nextWeight: Int? = null
    private var restOriginSeconds: Int = 0
    private var restAdjustedThisRest: Boolean = false
    private var showingGo: Boolean = false

    companion object {
        private const val ARG_EXERCISE = "exercise"
        private const val ARG_POSITION = "position"
        private const val ARG_NEXT_TITLE = "next_title"
        private const val ARG_NEXT_WEIGHT = "next_weight"

        /** Track exercises that were +/− adjusted twice so we can offer Save at end. */
        val pendingRestSaves = linkedMapOf<String, Int>()

        fun newInstance(
            exercise: Exercise,
            position: Int,
            nextTitle: String? = null,
            nextWeight: Int? = null
        ): ExerciseWorkoutFragment {
            val fragment = ExerciseWorkoutFragment()
            val args = Bundle()
            args.putSerializable(ARG_EXERCISE, exercise)
            args.putInt(ARG_POSITION, position)
            args.putString(ARG_NEXT_TITLE, nextTitle)
            if (nextWeight != null) args.putInt(ARG_NEXT_WEIGHT, nextWeight)
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
        nextTitle = arguments?.getString(ARG_NEXT_TITLE)
        nextWeight = if (arguments?.containsKey(ARG_NEXT_WEIGHT) == true) arguments?.getInt(ARG_NEXT_WEIGHT) else null
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
        syncDifficultySlider()

        binding.restMinusBtn.setOnClickListener { adjustRest(-15) }
        binding.restPlusBtn.setOnClickListener { adjustRest(15) }
        binding.restSkipBtn.setOnClickListener { skipRest() }
        binding.restGoText.setOnClickListener { dismissGo() }
        binding.restPanel.setOnClickListener { if (showingGo) dismissGo() }

        // Resume timer if it was running
        if (exercise.isTimerActive && exercise.timerEndTime > System.currentTimeMillis() && !isTimerRunning) {
            resumeTimer()
        }

        binding.titleText.setOnClickListener {
            val dialog = ExerciseEditorDialogFragment.newInstance(exercise) { updatedExercise ->
                val oldCompleted = exercise.completedSets
                exercise.title = updatedExercise.title
                exercise.notes = updatedExercise.notes
                exercise.restSec = updatedExercise.restSec
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

        binding.difficultySlider.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) {
                return@addOnChangeListener
            }
            val rating = sliderToRating(value)
            if (exercise.rating == rating) {
                return@addOnChangeListener
            }
            exercise.rating = rating
            updateDifficultyLabels()
            showRatingBubble(rating)
            onUpdate(position, exercise, exercise.completedSets, exercise.completedSets)
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
        syncDifficultySlider()
    }

    private fun startTimer(initialSeconds: Int = 59) {
        cancelTimerCallbacks()
        showingGo = false
        restAdjustedThisRest = false
        restOriginSeconds = initialSeconds.coerceAtLeast(0)
        exercise.isTimerActive = true
        exercise.timerDurationSeconds = initialSeconds
        exercise.remainingSeconds = initialSeconds
        exercise.timerEndTime = System.currentTimeMillis() + initialSeconds * 1000L
        isTimerRunning = true
        showRestPanel(counting = true)
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

    private fun showRestPanel(counting: Boolean) {
        if (_binding == null) return
        binding.restPanel.visibility = View.VISIBLE
        binding.restButtons.visibility = if (counting) View.VISIBLE else View.GONE
        binding.restGoText.visibility = if (counting) View.GONE else View.VISIBLE
        binding.restNextText.text = formatNextLine()
        binding.restSourceText.text = exercise.restSourceLabel.ifBlank {
            exercise.resolvedRest().sourceLabel
        }
    }

    private fun hideRestPanel() {
        if (_binding == null) return
        binding.restPanel.visibility = View.GONE
        showingGo = false
    }

    private fun adjustRest(delta: Int) {
        if (!isTimerRunning || showingGo) return
        val newRemaining = (exercise.remainingSeconds + delta).coerceIn(0, ExerciseTimers.MAX_REST_SEC)
        exercise.remainingSeconds = newRemaining
        exercise.timerDurationSeconds = maxOf(exercise.timerDurationSeconds, newRemaining)
        exercise.timerEndTime = System.currentTimeMillis() + newRemaining * 1000L
        if (!restAdjustedThisRest) {
            restAdjustedThisRest = true
        } else {
            // Second adjustment on same rest → offer save later
            pendingRestSaves[exercise.title] = newRemaining.coerceAtLeast(5)
        }
        if (newRemaining == 0) {
            onRestZero()
        } else {
            updateUI()
        }
    }

    private fun skipRest() {
        onRestZero()
    }

    private fun onRestZero() {
        cancelTimerCallbacks()
        isTimerRunning = false
        exercise.remainingSeconds = 0
        showingGo = true
        // Vibrate
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
        showRestPanel(counting = false)
        // Brief accent flash via existing pie flash, then show Go (do not auto-start next set)
        startFlashing(keepGo = true)
        updateUI()
        onUpdate(position, exercise, exercise.completedSets, exercise.completedSets)
    }

    private fun dismissGo() {
        showingGo = false
        exercise.isTimerActive = false
        exercise.timerEndTime = 0
        exercise.timerDurationSeconds = 0
        hideRestPanel()
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

    private fun startFlashing(keepGo: Boolean = false) {
        var flashCount = 0
        flashRunnable = Runnable {
            try {
                if (_binding != null) {
                    val showing = flashCount % 2 == 0
                    binding.progressPieChart.setCenterCountdownVisible(showing)
                    binding.restPieChart.setCenterCountdownVisible(showing)
                }
                flashCount++
                if (flashCount < 6) {
                    handler.postDelayed(flashRunnable!!, 200)
                } else {
                    flashRunnable = null
                    if (!keepGo) {
                        exercise.isTimerActive = false
                        exercise.remainingSeconds = 0
                        isTimerRunning = false
                        hideRestPanel()
                    } else if (_binding != null) {
                        binding.progressPieChart.setCenterCountdownVisible(true)
                        binding.restPieChart.setCenterCountdownVisible(true)
                        binding.restPieChart.setCenterCountdown("Go")
                        binding.progressPieChart.setCenterCountdown("Go")
                    }
                    if (_binding != null) updateUI()
                    onUpdate(position, exercise, exercise.completedSets, exercise.completedSets)
                }
            } catch (e: Exception) {
                if (!keepGo) stopTimer()
            }
        }
        if (_binding != null) {
            binding.progressPieChart.setCenterCountdown(ExerciseTimers.formatMss(0))
            binding.restPieChart.setCenterCountdown(ExerciseTimers.formatMss(0))
        }
        handler.post(flashRunnable!!)
    }

    private fun cancelTimerCallbacks() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        flashRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
        flashRunnable = null
    }

    private fun stopTimer() {
        cancelTimerCallbacks()
        isTimerRunning = false
        showingGo = false
        exercise.isTimerActive = false
        exercise.remainingSeconds = 0
        exercise.timerEndTime = 0
        exercise.timerDurationSeconds = 0
        hideRestPanel()
        updateUI()
        onUpdate(position, exercise, exercise.completedSets, exercise.completedSets)
    }

    private fun showFullScreenNotes() {
        // Set the notes text (remove "Notes: " prefix if present)
        val notesText = exercise.notes.removePrefix("Notes: ").trim()
        binding.fullScreenNotesText.text = notesText

        // Hide all other UI elements
        binding.titleContainer.visibility = View.GONE
        binding.statsRow.visibility = View.GONE
        binding.difficultyBlock.visibility = View.GONE
        binding.notesText.visibility = View.GONE
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
        binding.difficultyBlock.visibility = View.VISIBLE
        binding.notesText.visibility = View.VISIBLE
        binding.progressPieChart.visibility = View.VISIBLE

        syncDifficultySlider()
        updateUI()
    }

    private fun sliderToRating(value: Float): String = when (value.toInt()) {
        0 -> "easy"
        2 -> "hard"
        else -> "good"
    }

    private fun ratingToSlider(rating: String): Float = when (rating) {
        "easy" -> 0f
        "hard" -> 2f
        else -> 1f
    }

    private fun syncDifficultySlider() {
        if (_binding == null) return
        val target = ratingToSlider(exercise.rating)
        if (binding.difficultySlider.value != target) {
            binding.difficultySlider.value = target
        }
        updateDifficultyLabels()
    }

    private fun updateDifficultyLabels() {
        if (_binding == null) return
        val selected = ratingToSlider(exercise.rating).toInt()
        val labels = listOf(binding.labelEasy, binding.labelGood, binding.labelHard)
        labels.forEachIndexed { index, label ->
            val active = index == selected
            label.setTextColor(if (active) 0xFFFFFFFF.toInt() else 0xFF9E9E9E.toInt())
            label.setTypeface(null, if (active) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun showRatingBubble(rating: String) {
        val emoji = when (rating) {
            "easy" -> "🥱"
            "good" -> "💪"
            "hard" -> "🥵"
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

    private fun formatNextLine(): String {
        return when {
            !nextTitle.isNullOrBlank() && nextWeight != null ->
                "Next · $nextTitle · ${nextWeight} lb"
            !nextTitle.isNullOrBlank() -> "Next · $nextTitle"
            else -> "Next set"
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
            binding.restPieChart.setCenterCountdown("Go")
            binding.restPieChart.setCountdownRing(0, 1)
            binding.restPanel.visibility = View.VISIBLE
            binding.restButtons.visibility = View.GONE
            binding.restGoText.visibility = View.VISIBLE
        } else if (exercise.isTimerActive && isTimerRunning && exercise.remainingSeconds > 0) {
            val label = ExerciseTimers.formatMss(exercise.remainingSeconds)
            binding.progressPieChart.setCenterCountdown(label)
            binding.restPieChart.setCenterCountdown(label)
            val dur = exercise.timerDurationSeconds.coerceAtLeast(exercise.remainingSeconds).coerceAtLeast(1)
            binding.restPieChart.setCountdownRing(exercise.remainingSeconds, dur)
            binding.restPanel.visibility = View.VISIBLE
            binding.restButtons.visibility = View.VISIBLE
            binding.restGoText.visibility = View.GONE
            binding.restNextText.text = formatNextLine()
            binding.restSourceText.text = exercise.restSourceLabel.ifBlank {
                exercise.resolvedRest().sourceLabel
            }
        } else if (flashRunnable == null) {
            binding.progressPieChart.setCenterCountdown(null)
            binding.restPieChart.clearCountdownRing()
            if (!showingGo) binding.restPanel.visibility = View.GONE
        }

        binding.notesText.text = if (exercise.notes.isNotBlank()) "Notes: ${exercise.notes}" else ""

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
